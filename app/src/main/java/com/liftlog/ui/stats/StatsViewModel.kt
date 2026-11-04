package com.liftlog.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liftlog.data.db.ExerciseDao
import com.liftlog.data.db.ProteinDao
import com.liftlog.data.db.SetEntryDao
import com.liftlog.data.db.WorkoutDao
import com.liftlog.data.entity.BodyweightEntry
import com.liftlog.data.entity.Exercise
import com.liftlog.data.entity.ProteinEntry
import com.liftlog.data.entity.SetEntry
import com.liftlog.data.entity.Workout
import com.liftlog.data.repository.BodyweightRepository
import com.liftlog.data.repository.SettingsRepository
import com.liftlog.data.repository.UserSettings
import com.liftlog.engine.E1RMCalculator
import com.liftlog.engine.ProgressionEngine
import com.liftlog.engine.SessionData
import com.liftlog.engine.SetData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class StatsTimeframe {
    WEEKLY,
    MONTHLY
}

data class ExerciseSessionComparison(
    val exercise: Exercise,
    val latestTopSetText: String, // e.g. "80kg × 7"
    val latestE1rm: Double?,
    val previousTopSetText: String?, // e.g. "77.5kg × 8"
    val previousE1rm: Double?,
    val e1rmDeltaPercent: Double? // e.g. +3.5%
)

data class MuscleSetVolume(
    val muscleName: String,
    val setsCount: Double,
    val targetSets: Int
)

data class DailyProteinBar(
    val dayLabel: String,
    val grams: Double,
    val timestamp: Long
)

data class BodyweightPoint(
    val dateLabel: String,
    val kg: Double,
    val timestamp: Long
)

data class E1RMPoint(
    val dateLabel: String,
    val e1rm: Double,
    val timestamp: Long
)

data class StatsUiState(
    val timeframe: StatsTimeframe = StatsTimeframe.WEEKLY,
    val sessionsDone: Int = 0,
    val sessionsTarget: Int = 4,
    val muscleVolumes: List<MuscleSetVolume> = emptyList(),
    val exerciseComparisons: List<ExerciseSessionComparison> = emptyList(),
    val proteinBars: List<DailyProteinBar> = emptyList(),
    val proteinTarget: Double = 110.0,
    val proteinAverage: Double = 0.0,
    val proteinDaysHit: Int = 0,
    val proteinDaysTracked: Int = 0,
    val bodyweightTrend: List<BodyweightPoint> = emptyList(),
    val bodyweight7DayAvg: Double = 0.0,
    val selectedExerciseIdForChart: Long? = null,
    val exerciseE1rmPoints: List<E1RMPoint> = emptyList(),
    val allActiveExercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = true
)

private data class StatsDataBundle(
    val workouts: List<Workout>,
    val sets: List<SetEntry>,
    val proteinEntries: List<ProteinEntry>,
    val bwEntries: List<BodyweightEntry>,
    val settings: UserSettings
)

class StatsViewModel(
    private val workoutDao: WorkoutDao,
    private val setEntryDao: SetEntryDao,
    private val exerciseDao: ExerciseDao,
    private val proteinDao: ProteinDao,
    private val bodyweightRepository: BodyweightRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _timeframe = MutableStateFlow(StatsTimeframe.WEEKLY)
    private val _selectedExerciseIdForChart = MutableStateFlow<Long?>(null)
    private val _manualRefreshTrigger = MutableStateFlow(0L)

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            val baseFlow = combine(
                workoutDao.getCompletedWorkoutsFlow(),
                setEntryDao.getAllSetsFlow(),
                proteinDao.getAllEntries(),
                bodyweightRepository.getAllEntries(),
                settingsRepository.settingsFlow
            ) { workouts, sets, proteinEntries, bwEntries, settings ->
                StatsDataBundle(workouts, sets, proteinEntries, bwEntries, settings)
            }

            combine(
                baseFlow,
                _timeframe,
                _selectedExerciseIdForChart,
                _manualRefreshTrigger
            ) { bundle, timeframe, selectedExId, _ ->
                Triple(bundle, timeframe, selectedExId)
            }.collectLatest { (bundle, timeframe, selectedExId) ->
                val calculatedState = calculateStats(bundle, timeframe, selectedExId)
                _uiState.value = calculatedState
            }
        }
    }

    fun setTimeframe(timeframe: StatsTimeframe) {
        _timeframe.value = timeframe
    }

    fun selectExerciseForChart(exerciseId: Long) {
        _selectedExerciseIdForChart.value = exerciseId
    }

    fun loadStats() {
        _manualRefreshTrigger.value = System.currentTimeMillis()
    }

    private suspend fun calculateStats(
        bundle: StatsDataBundle,
        timeframe: StatsTimeframe,
        selectedExerciseId: Long?
    ): StatsUiState {
        val now = System.currentTimeMillis()
        val isWeekly = timeframe == StatsTimeframe.WEEKLY
        val daysCount = if (isWeekly) 7 else 30
        val windowStartTs = now - (daysCount * 24L * 60 * 60 * 1000)

        val settings = bundle.settings
        val allSets = bundle.sets
        val workouts = bundle.workouts
        val proteinEntries = bundle.proteinEntries
        val bwEntries = bundle.bwEntries

        // 1. Sessions done in window
        val completedInWindow = workouts.filter { workout ->
            workout.startTs in windowStartTs..now &&
                    workout.endTs != null &&
                    !workout.isDiscarded
        }
        val sessionsDone = completedInWindow.size
        val sessionsTarget = if (isWeekly) settings.sessionsPerWeekTarget else settings.sessionsPerWeekTarget * 4

        // 2. Hard sets per muscle (working sets only, secondary = 0.5, pain excluded, swung counts)
        val setsInWindow = allSets.filter { it.ts in windowStartTs..now }
        val exercises = exerciseDao.getAllExercises().first().associateBy { it.id }

        val muscleSetCounts = mutableMapOf<String, Double>()
        for (set in setsInWindow) {
            if (set.type != "working" || set.flagPain) continue
            val ex = exercises[set.exerciseId] ?: continue

            // Primary muscles: 1 set
            val primaries = ex.primaryMuscles.split(",").map { it.trim() }.filter { it.isNotBlank() }
            for (m in primaries) {
                muscleSetCounts[m] = (muscleSetCounts[m] ?: 0.0) + 1.0
            }

            // Secondary muscles: 0.5 set
            val secondaries = ex.secondaryMuscles.split(",").map { it.trim() }.filter { it.isNotBlank() }
            for (m in secondaries) {
                muscleSetCounts[m] = (muscleSetCounts[m] ?: 0.0) + 0.5
            }
        }

        val muscleVolumes = muscleSetCounts.map { (muscle, count) ->
            MuscleSetVolume(
                muscleName = muscle,
                setsCount = count,
                targetSets = if (isWeekly) settings.weeklySetTarget else settings.weeklySetTarget * 4
            )
        }.sortedByDescending { it.setsCount }

        // 3. Exercise comparisons (latest session vs previous session)
        val bwAvg = bodyweightRepository.getRecent7DayAverage()
        val comparisons = mutableListOf<ExerciseSessionComparison>()

        val setsByExercise = allSets.groupBy { it.exerciseId }
        for ((exId, exSets) in setsByExercise) {
            val ex = exercises[exId] ?: continue
            val sessions = exSets.groupBy { it.workoutId }.map { (_, sList) ->
                SessionData(
                    timestamp = sList.first().ts,
                    sets = sList.map { SetData(it.weightKg, it.reps, it.type, it.rpe, it.flagSwung, it.flagPain, it.ts) }
                )
            }.sortedByDescending { it.timestamp }

            if (sessions.isNotEmpty()) {
                val latestTop = ProgressionEngine.findTopSet(sessions[0].sets, ex.bwFactor, bwAvg)
                if (latestTop != null) {
                    val latestE1rm = E1RMCalculator.calculateE1RM(latestTop.weightKg, latestTop.reps, latestTop.rpe, ex.bwFactor, bwAvg)
                    val prevTop = if (sessions.size >= 2) ProgressionEngine.findTopSet(sessions[1].sets, ex.bwFactor, bwAvg) else null
                    val prevE1rm = if (prevTop != null) E1RMCalculator.calculateE1RM(prevTop.weightKg, prevTop.reps, prevTop.rpe, ex.bwFactor, bwAvg) else null

                    val deltaPercent = if (latestE1rm != null && prevE1rm != null && prevE1rm > 0.0) {
                        ((latestE1rm - prevE1rm) / prevE1rm) * 100.0
                    } else null

                    comparisons.add(
                        ExerciseSessionComparison(
                            exercise = ex,
                            latestTopSetText = "${latestTop.weightKg}kg × ${latestTop.reps}",
                            latestE1rm = latestE1rm,
                            previousTopSetText = if (prevTop != null) "${prevTop.weightKg}kg × ${prevTop.reps}" else null,
                            previousE1rm = prevE1rm,
                            e1rmDeltaPercent = deltaPercent
                        )
                    )
                }
            }
        }

        // 4. Daily protein bars + target line + accurate average on tracked days
        val proteinBars = mutableListOf<DailyProteinBar>()
        var hitDaysCount = 0
        var sumProtein = 0.0

        val sdf = SimpleDateFormat("EEE", Locale.US)
        for (i in (daysCount - 1) downTo 0) {
            val dayTs = now - (i * 24L * 60 * 60 * 1000)
            val (dStart, dEnd) = com.liftlog.data.repository.ProteinRepository.getDayWindow(dayTs, settings.dayRolloverHour)
            val dayEntries = proteinEntries.filter { it.ts in dStart until dEnd }
            val daySum = dayEntries.sumOf { it.grams }
            sumProtein += daySum
            if (daySum >= settings.proteinTarget) {
                hitDaysCount++
            }
            val label = sdf.format(Date(dayTs))
            proteinBars.add(DailyProteinBar(dayLabel = label, grams = daySum, timestamp = dayTs))
        }

        // Calculate average only over days where protein was actually logged
        val daysWithProtein = proteinBars.count { it.grams > 0.0 }
        val avgProtein = if (daysWithProtein > 0) sumProtein / daysWithProtein else 0.0

        // 5. Bodyweight trend
        val bwTrend = bwEntries
            .filter { it.ts >= windowStartTs }
            .sortedBy { it.ts }
            .map {
                val dateLabel = SimpleDateFormat("MM/dd", Locale.US).format(Date(it.ts))
                BodyweightPoint(dateLabel = dateLabel, kg = it.kg, timestamp = it.ts)
            }

        val activeExList = exercises.values.filter { !it.archived }.sortedBy { it.name }
        val effectiveSelectedExerciseId = selectedExerciseId ?: activeExList.firstOrNull()?.id

        // 6. E1RM chart points for selected exercise
        val points = mutableListOf<E1RMPoint>()
        if (effectiveSelectedExerciseId != null) {
            val targetEx = exercises[effectiveSelectedExerciseId]
            if (targetEx != null) {
                val targetSets = allSets.filter { it.exerciseId == effectiveSelectedExerciseId }
                val targetSessions = targetSets.groupBy { it.workoutId }.map { (_, sList) ->
                    SessionData(
                        timestamp = sList.first().ts,
                        sets = sList.map { SetData(it.weightKg, it.reps, it.type, it.rpe, it.flagSwung, it.flagPain, it.ts) }
                    )
                }.sortedBy { it.timestamp }

                val chartSdf = SimpleDateFormat("MM/dd", Locale.US)
                for (session in targetSessions) {
                    val top = ProgressionEngine.findTopSet(session.sets, targetEx.bwFactor, bwAvg)
                    if (top != null) {
                        val e1rm = E1RMCalculator.calculateE1RM(top.weightKg, top.reps, top.rpe, targetEx.bwFactor, bwAvg)
                        if (e1rm != null && e1rm > 0.0) {
                            points.add(
                                E1RMPoint(
                                    dateLabel = chartSdf.format(Date(session.timestamp)),
                                    e1rm = e1rm,
                                    timestamp = session.timestamp
                                )
                            )
                        }
                    }
                }
            }
        }

        return StatsUiState(
            timeframe = timeframe,
            sessionsDone = sessionsDone,
            sessionsTarget = sessionsTarget,
            muscleVolumes = muscleVolumes,
            exerciseComparisons = comparisons.sortedBy { it.exercise.name },
            proteinBars = proteinBars,
            proteinTarget = settings.proteinTarget,
            proteinAverage = avgProtein,
            proteinDaysHit = hitDaysCount,
            proteinDaysTracked = daysWithProtein,
            bodyweightTrend = bwTrend,
            bodyweight7DayAvg = bwAvg,
            selectedExerciseIdForChart = effectiveSelectedExerciseId,
            exerciseE1rmPoints = points,
            allActiveExercises = activeExList,
            isLoading = false
        )
    }

    class Factory(
        private val workoutDao: WorkoutDao,
        private val setEntryDao: SetEntryDao,
        private val exerciseDao: ExerciseDao,
        private val proteinDao: ProteinDao,
        private val bodyweightRepository: BodyweightRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatsViewModel(workoutDao, setEntryDao, exerciseDao, proteinDao, bodyweightRepository, settingsRepository) as T
        }
    }
}
