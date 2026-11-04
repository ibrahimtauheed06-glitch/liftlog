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
import com.liftlog.data.repository.BodyweightRepository
import com.liftlog.data.repository.SettingsRepository
import com.liftlog.engine.E1RMCalculator
import com.liftlog.engine.ProgressionEngine
import com.liftlog.engine.SessionData
import com.liftlog.engine.SetData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

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
    val bodyweightTrend: List<BodyweightPoint> = emptyList(),
    val bodyweight7DayAvg: Double = 0.0,
    val selectedExerciseIdForChart: Long? = null,
    val exerciseE1rmPoints: List<E1RMPoint> = emptyList(),
    val allActiveExercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = true
)

class StatsViewModel(
    private val workoutDao: WorkoutDao,
    private val setEntryDao: SetEntryDao,
    private val exerciseDao: ExerciseDao,
    private val proteinDao: ProteinDao,
    private val bodyweightRepository: BodyweightRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun setTimeframe(timeframe: StatsTimeframe) {
        _uiState.value = _uiState.value.copy(timeframe = timeframe)
        loadStats()
    }

    fun selectExerciseForChart(exerciseId: Long) {
        _uiState.value = _uiState.value.copy(selectedExerciseIdForChart = exerciseId)
        loadExerciseChart(exerciseId)
    }

    fun loadStats() {
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            val now = System.currentTimeMillis()
            val isWeekly = _uiState.value.timeframe == StatsTimeframe.WEEKLY
            val daysCount = if (isWeekly) 7 else 30
            val windowStartTs = now - (daysCount * 24L * 60 * 60 * 1000)

            // 1. Sessions done in window
            val completedWorkouts = workoutDao.getCompletedWorkoutsBetween(windowStartTs, now)
            val sessionsDone = completedWorkouts.size
            val sessionsTarget = if (isWeekly) settings.sessionsPerWeekTarget else settings.sessionsPerWeekTarget * 4

            // 2. Hard sets per muscle (working sets only, secondary = 0.5, pain excluded, swung counts)
            val setsInWindow = setEntryDao.getSetsBetween(windowStartTs, now)
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

            val allSets = setEntryDao.getAllSetsSync()
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

            // 4. Daily protein bars + target line
            val proteinEntries = proteinDao.getAllEntriesSync()
            val proteinBars = mutableListOf<DailyProteinBar>()
            var hitDaysCount = 0
            var sumProtein = 0.0

            val sdf = java.text.SimpleDateFormat("EEE", java.util.Locale.US)
            for (i in (daysCount - 1) downTo 0) {
                val dayTs = now - (i * 24L * 60 * 60 * 1000)
                val (dStart, dEnd) = com.liftlog.data.repository.ProteinRepository.getDayWindow(dayTs, settings.dayRolloverHour)
                val dayEntries = proteinEntries.filter { it.ts in dStart until dEnd }
                val daySum = dayEntries.sumOf { it.grams }
                sumProtein += daySum
                if (daySum >= settings.proteinTarget) {
                    hitDaysCount++
                }
                val label = sdf.format(java.util.Date(dayTs))
                proteinBars.add(DailyProteinBar(dayLabel = label, grams = daySum, timestamp = dayTs))
            }
            val avgProtein = if (daysCount > 0) sumProtein / daysCount else 0.0

            // 5. Bodyweight trend
            val bwEntries = bodyweightRepository.getAllEntries().first()
            val bwTrend = bwEntries
                .filter { it.ts >= windowStartTs }
                .sortedBy { it.ts }
                .map {
                    val dateLabel = java.text.SimpleDateFormat("MM/dd", java.util.Locale.US).format(java.util.Date(it.ts))
                    BodyweightPoint(dateLabel = dateLabel, kg = it.kg, timestamp = it.ts)
                }

            val activeExList = exercises.values.filter { !it.archived }.sortedBy { it.name }
            val selectedId = _uiState.value.selectedExerciseIdForChart ?: activeExList.firstOrNull()?.id

            _uiState.value = _uiState.value.copy(
                sessionsDone = sessionsDone,
                sessionsTarget = sessionsTarget,
                muscleVolumes = muscleVolumes,
                exerciseComparisons = comparisons.sortedBy { it.exercise.name },
                proteinBars = proteinBars,
                proteinTarget = settings.proteinTarget,
                proteinAverage = avgProtein,
                proteinDaysHit = hitDaysCount,
                bodyweightTrend = bwTrend,
                bodyweight7DayAvg = bwAvg,
                selectedExerciseIdForChart = selectedId,
                allActiveExercises = activeExList,
                isLoading = false
            )

            if (selectedId != null) {
                loadExerciseChart(selectedId)
            }
        }
    }

    private fun loadExerciseChart(exerciseId: Long) {
        viewModelScope.launch {
            val ex = exerciseDao.getExerciseById(exerciseId) ?: return@launch
            val sets = setEntryDao.getSetsForExerciseSync(exerciseId)
            val bwAvg = bodyweightRepository.getRecent7DayAverage()

            val sessions = sets.groupBy { it.workoutId }.map { (_, sList) ->
                SessionData(
                    timestamp = sList.first().ts,
                    sets = sList.map { SetData(it.weightKg, it.reps, it.type, it.rpe, it.flagSwung, it.flagPain, it.ts) }
                )
            }.sortedBy { it.timestamp }

            val points = mutableListOf<E1RMPoint>()
            val sdf = java.text.SimpleDateFormat("MM/dd", java.util.Locale.US)

            for (session in sessions) {
                val top = ProgressionEngine.findTopSet(session.sets, ex.bwFactor, bwAvg)
                if (top != null) {
                    val e1rm = E1RMCalculator.calculateE1RM(top.weightKg, top.reps, top.rpe, ex.bwFactor, bwAvg)
                    if (e1rm != null && e1rm > 0.0) {
                        points.add(
                            E1RMPoint(
                                dateLabel = sdf.format(java.util.Date(session.timestamp)),
                                e1rm = e1rm,
                                timestamp = session.timestamp
                            )
                        )
                    }
                }
            }

            _uiState.value = _uiState.value.copy(exerciseE1rmPoints = points)
        }
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
