package com.liftlog.data.repository

import com.liftlog.data.db.BodyweightDao
import com.liftlog.data.db.ExerciseDao
import com.liftlog.data.db.SetEntryDao
import com.liftlog.data.db.TemplateDao
import com.liftlog.data.db.WorkoutDao
import com.liftlog.data.entity.Exercise
import com.liftlog.data.entity.SetEntry
import com.liftlog.data.entity.TemplateSlot
import com.liftlog.data.entity.Workout
import com.liftlog.engine.ExerciseConfig
import com.liftlog.engine.NextTargetResult
import com.liftlog.engine.PRDetector
import com.liftlog.engine.PRResult
import com.liftlog.engine.ProgressionEngine
import com.liftlog.engine.SessionData
import com.liftlog.engine.SetData
import com.liftlog.engine.WarmupCalculator
import com.liftlog.engine.WarmupSetTarget
import com.liftlog.engine.WorkoutPhase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class ExerciseInWorkout(
    val exercise: Exercise,
    val slot: TemplateSlot?,
    val plannedSets: Int,
    val target: NextTargetResult,
    val warmupTargets: List<WarmupSetTarget>,
    val loggedSets: List<SetEntry>,
    val lastSessionSummary: String?, // e.g. "Last: 65x7,7,6 (RPE9)"
    val hasPainWarning: Boolean = false,
    val bodyweightAvg: Double = 0.0
)

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val setEntryDao: SetEntryDao,
    private val exerciseDao: ExerciseDao,
    private val templateDao: TemplateDao,
    private val bodyweightDao: BodyweightDao,
    private val settingsRepository: SettingsRepository
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getActiveWorkoutFlow(): Flow<Workout?> = settingsRepository.settingsFlow.flatMapLatest { settings ->
        val activeId = settings.activeWorkoutId
        if (activeId == null) {
            flowOf(null)
        } else {
            workoutDao.getWorkoutByIdFlow(activeId).map { workout ->
                if (workout != null && workout.endTs == null && !workout.isDiscarded) workout else null
            }
        }
    }

    fun getSetsForWorkoutFlow(workoutId: Long): Flow<List<SetEntry>> = setEntryDao.getSetsForWorkout(workoutId)

    suspend fun getActiveWorkout(): Workout? {
        val settings = settingsRepository.settingsFlow.first()
        val activeId = settings.activeWorkoutId ?: return null
        val workout = workoutDao.getWorkoutById(activeId)
        return if (workout != null && workout.endTs == null && !workout.isDiscarded) workout else null
    }

    suspend fun getWorkoutById(workoutId: Long): Workout? = workoutDao.getWorkoutById(workoutId)

    fun getCompletedWorkoutsFlow(): Flow<List<Workout>> = workoutDao.getCompletedWorkoutsFlow()

    suspend fun cleanAllEmptyWorkouts(excludeActiveId: Long = -1L) {
        workoutDao.cleanAllEmptyWorkouts(excludeActiveId)
    }

    suspend fun startWorkout(templateId: Long?): Long {
        cleanAllEmptyWorkouts()
        val now = System.currentTimeMillis()
        val workout = Workout(
            templateId = templateId,
            startTs = now,
            endTs = null,
            note = null
        )
        val workoutId = workoutDao.insertWorkout(workout)
        settingsRepository.setActiveWorkoutId(workoutId)
        return workoutId
    }

    suspend fun finishWorkout(workoutId: Long, note: String?) {
        val loggedSets = setEntryDao.getSetsForWorkoutSync(workoutId)
        if (loggedSets.isEmpty()) {
            workoutDao.deleteWorkout(workoutId)
        } else {
            val now = System.currentTimeMillis()
            workoutDao.completeWorkout(workoutId, now, note)
        }
        settingsRepository.setActiveWorkoutId(null)
        cleanAllEmptyWorkouts()
    }

    suspend fun cancelActiveWorkout(workoutId: Long) {
        workoutDao.deleteWorkout(workoutId)
        settingsRepository.setActiveWorkoutId(null)
        cleanAllEmptyWorkouts()
    }

    fun getDiscardedWorkoutsFlow(): Flow<List<Workout>> = workoutDao.getDiscardedWorkoutsFlow()

    suspend fun discardWorkout(workoutId: Long) {
        val now = System.currentTimeMillis()
        val loggedSets = setEntryDao.getSetsForWorkoutSync(workoutId)
        if (loggedSets.isEmpty()) {
            workoutDao.deleteWorkout(workoutId)
        } else {
            workoutDao.discardWorkout(workoutId, now)
        }
        settingsRepository.setActiveWorkoutId(null)
        cleanAllEmptyWorkouts()
    }

    suspend fun cleanEmptyUnfinishedWorkouts() {
        cleanAllEmptyWorkouts()
    }

    suspend fun deleteExerciseFromWorkout(workoutId: Long, exerciseId: Long) {
        setEntryDao.deleteSetsForExerciseInWorkout(workoutId, exerciseId)
    }

    suspend fun updateWorkoutNote(workoutId: Long, note: String?) {
        val workout = workoutDao.getWorkoutById(workoutId) ?: return
        workoutDao.updateWorkout(workout.copy(note = note))
    }

    suspend fun restoreWorkout(workoutId: Long) {
        workoutDao.restoreWorkout(workoutId)
    }

    suspend fun permanentlyDeleteWorkout(workoutId: Long) {
        workoutDao.deleteWorkout(workoutId)
    }

    suspend fun logSet(
        workoutId: Long,
        exerciseId: Long,
        weightKg: Double,
        reps: Int,
        type: String = "working",
        rpe: Double? = null,
        flagSwung: Boolean = false,
        flagPain: Boolean = false,
        note: String? = null
    ): Pair<Long, PRResult> {
        val now = System.currentTimeMillis()
        val set = SetEntry(
            workoutId = workoutId,
            exerciseId = exerciseId,
            ts = now,
            weightKg = weightKg,
            reps = reps,
            type = type,
            rpe = rpe,
            flagSwung = flagSwung,
            flagPain = flagPain,
            note = note
        )
        val setId = setEntryDao.insertSet(set)

        // Persist active workout on every logged set so it survives process death and resumes exactly
        settingsRepository.setActiveWorkoutId(workoutId)

        // Check for PR
        val exercise = exerciseDao.getExerciseById(exerciseId)
        val bwFactor = exercise?.bwFactor ?: 0.0
        val bwAvg = getSevenDayAverageBodyweight()
        val historySets = getHistoricalSets(exerciseId).filter { it.id != setId }

        val prResult = PRDetector.checkPR(
            newSet = SetData(weightKg, reps, type, rpe, flagSwung, flagPain, now),
            historicalSets = historySets.map {
                SetData(it.weightKg, it.reps, it.type, it.rpe, it.flagSwung, it.flagPain, it.ts)
            },
            bwFactor = bwFactor,
            bodyweight7DayAvg = bwAvg
        )

        return Pair(setId, prResult)
    }

    suspend fun updateSet(set: SetEntry) {
        setEntryDao.updateSet(set)
    }

    suspend fun deleteSet(setId: Long) {
        setEntryDao.deleteSet(setId)
    }

    suspend fun getHistoricalSets(exerciseId: Long): List<SetEntry> {
        // Look up variants as well if any
        val variants = exerciseDao.getVariants(exerciseId).map { it.id }
        val allIds = listOf(exerciseId) + variants
        return setEntryDao.getSetsForExercisesSync(allIds)
    }

    suspend fun getSevenDayAverageBodyweight(): Double {
        val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        return bodyweightDao.getAverageWeightSince(sevenDaysAgo) ?: 0.0
    }

    suspend fun prepareWorkoutExercises(workoutId: Long): List<ExerciseInWorkout> {
        val workout = workoutDao.getWorkoutById(workoutId) ?: return emptyList()
        val templateId = workout.templateId
        val slots = if (templateId != null) {
            templateDao.getSlotsForTemplateSync(templateId)
        } else {
            emptyList()
        }

        val settings = settingsRepository.settingsFlow.first()
        val isDeloadActive = settings.deloadActiveUntilTs > System.currentTimeMillis()
        val phase = if (settings.phase.equals("HOME", ignoreCase = true)) WorkoutPhase.HOME else WorkoutPhase.GYM
        val bwAvg = getSevenDayAverageBodyweight()

        val allLoggedSets = setEntryDao.getSetsForWorkoutSync(workoutId)
        val exerciseIdsInWorkout = (slots.map { it.exerciseId } + allLoggedSets.map { it.exerciseId }).distinct()

        val exercises = exerciseDao.getExercisesByIds(exerciseIdsInWorkout).associateBy { it.id }
        val result = mutableListOf<ExerciseInWorkout>()

        var isFirstCompoundFound = false

        for (exerciseId in exerciseIdsInWorkout) {
            val ex = exercises[exerciseId] ?: continue
            val slot = slots.find { it.exerciseId == exerciseId }
            val loggedForEx = allLoggedSets.filter { it.exerciseId == exerciseId }

            // Group history into sessions
            val history = getHistoricalSets(exerciseId).filter { it.workoutId != workoutId }
            val sessionsByWorkout = history.groupBy { it.workoutId }
            val sessionsData = sessionsByWorkout.map { (_, sets) ->
                SessionData(
                    timestamp = sets.first().ts,
                    sets = sets.map { SetData(it.weightKg, it.reps, it.type, it.rpe, it.flagSwung, it.flagPain, it.ts) }
                )
            }.sortedByDescending { it.timestamp }

            val config = ExerciseConfig(
                id = ex.id,
                name = ex.name,
                equipment = ex.equipment,
                incrementKg = ex.incrementKg,
                plateKg = ex.plateKg,
                repLo = ex.repLo,
                repHi = ex.repHi,
                defaultSets = ex.defaultSets,
                restSec = ex.restSec,
                isCompound = ex.isCompound,
                bwFactor = ex.bwFactor,
                homeCapKg = ex.homeCapKg
            )

            val computedTarget = ProgressionEngine.computeNextTarget(
                exercise = config,
                sessions = sessionsData,
                currentPhase = phase,
                isDeloadActive = isDeloadActive,
                bodyweight7DayAvg = bwAvg
            )

            val nextTarget = if (sessionsData.isEmpty() && ex.defaultWeightKg > 0.0 && computedTarget.targetWeightKg == 0.0) {
                computedTarget.copy(targetWeightKg = ex.defaultWeightKg)
            } else {
                computedTarget
            }

            // Warmup calculation:
            // "The first compound of each workout gets one prefilled warm-up set at 60% of the target weight"
            // (or 2-set ramp if enabled in settings)
            val warmups = if (!isFirstCompoundFound && ex.isCompound) {
                isFirstCompoundFound = true
                WarmupCalculator.calculateWarmupSets(
                    targetWeightKg = nextTarget.targetWeightKg,
                    incrementKg = ex.incrementKg,
                    twoSetRamp = settings.twoSetWarmup,
                    targetRepLo = ex.repLo
                )
            } else {
                emptyList()
            }

            // Summary of last session: e.g. "Last: 65x7,7,6 (RPE9)"
            val lastSession = sessionsData.firstOrNull()
            val lastSummary = if (lastSession != null) {
                val workingSets = lastSession.sets.filter { it.type == "working" }
                if (workingSets.isNotEmpty()) {
                    val weightStr = workingSets.first().weightKg.let { w ->
                        if (w % 1.0 == 0.0) w.toInt().toString() else w.toString()
                    }
                    val repsStr = workingSets.joinToString(",") { it.reps.toString() }
                    val maxRpe = workingSets.mapNotNull { it.rpe }.maxOrNull()
                    val rpeStr = if (maxRpe != null) " (RPE$maxRpe)" else ""
                    "Last: ${weightStr}x$repsStr$rpeStr"
                } else null
            } else null

            // Pain warning: 2+ pain flags on one exercise in 14 days
            val painWarning = com.liftlog.engine.DeloadDetector.hasPainWarning(sessionsData)

            result.add(
                ExerciseInWorkout(
                    exercise = ex,
                    slot = slot,
                    plannedSets = nextTarget.setsCount,
                    target = nextTarget,
                    warmupTargets = warmups,
                    loggedSets = loggedForEx,
                    lastSessionSummary = lastSummary,
                    hasPainWarning = painWarning,
                    bodyweightAvg = bwAvg
                )
            )
        }

        return result
    }
}
