package com.liftlog.ui.train

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liftlog.data.entity.SetEntry
import com.liftlog.data.repository.ExerciseInWorkout
import com.liftlog.data.repository.SettingsRepository
import com.liftlog.data.repository.WorkoutRepository
import com.liftlog.engine.PRResult
import com.liftlog.service.RestTimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ActiveWorkoutUiState(
    val workoutId: Long,
    val workout: com.liftlog.data.entity.Workout? = null,
    val exercises: List<ExerciseInWorkout> = emptyList(),
    val prefilledInputs: Map<Long, CurrentSetInput> = emptyMap(), // exerciseId -> CurrentSetInput
    val prBanner: PRResult? = null,
    val isFinishing: Boolean = false,
    val isLoading: Boolean = true
)

data class CurrentSetInput(
    val weightKg: Double,
    val reps: Int,
    val type: String = "working", // "working" | "warmup" | "drop"
    val rpe: Double = 9.0,
    val flagSwung: Boolean = false,
    val flagPain: Boolean = false,
    val note: String? = null
)

class ActiveWorkoutViewModel(
    val workoutId: Long,
    private val workoutRepository: WorkoutRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState(workoutId = workoutId))
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    init {
        loadWorkoutData()
    }

    fun loadWorkoutData() {
        viewModelScope.launch {
            val workout = workoutRepository.getWorkoutById(workoutId)
            val exercisesInWorkout = workoutRepository.prepareWorkoutExercises(workoutId)
            val currentInputs = _uiState.value.prefilledInputs.toMutableMap()

            // Prefill inputs for exercises that don't have them yet or advance after logging
            for (exItem in exercisesInWorkout) {
                val exId = exItem.exercise.id
                val loggedCount = exItem.loggedSets.size
                val warmupCount = exItem.warmupTargets.size

                val defaultType = if (loggedCount < warmupCount) "warmup" else "working"
                val defaultWeight = if (loggedCount < warmupCount) {
                    exItem.warmupTargets[loggedCount].weightKg
                } else {
                    exItem.target.targetWeightKg
                }
                val defaultReps = if (loggedCount < warmupCount) {
                    exItem.warmupTargets[loggedCount].reps
                } else {
                    exItem.target.targetReps
                }

                // If input doesn't exist yet, initialize it
                if (!currentInputs.containsKey(exId)) {
                    currentInputs[exId] = CurrentSetInput(
                        weightKg = defaultWeight,
                        reps = defaultReps,
                        type = defaultType,
                        rpe = 9.0
                    )
                }
            }

            _uiState.value = _uiState.value.copy(
                workout = workout,
                exercises = exercisesInWorkout,
                prefilledInputs = currentInputs,
                isLoading = false
            )
        }
    }

    fun setWeightDirect(exerciseId: Long, weight: Double) {
        updateInput(exerciseId) { it.copy(weightKg = maxOf(0.0, weight)) }
    }

    fun setRepsDirect(exerciseId: Long, reps: Int) {
        updateInput(exerciseId) { it.copy(reps = maxOf(1, reps)) }
    }

    fun discardWorkout(context: Context, onDiscarded: () -> Unit) {
        viewModelScope.launch {
            RestTimerService.stopTimer(context)
            workoutRepository.discardWorkout(workoutId)
            onDiscarded()
        }
    }

    fun updateInput(exerciseId: Long, transform: (CurrentSetInput) -> CurrentSetInput) {
        val current = _uiState.value.prefilledInputs[exerciseId] ?: return
        val updated = transform(current)
        val newMap = _uiState.value.prefilledInputs.toMutableMap()
        newMap[exerciseId] = updated
        _uiState.value = _uiState.value.copy(prefilledInputs = newMap)
    }

    fun adjustWeight(exerciseId: Long, delta: Double) {
        updateInput(exerciseId) { input ->
            val ex = _uiState.value.exercises.find { it.exercise.id == exerciseId }?.exercise
            val increment = ex?.incrementKg ?: 2.5
            val newWeight = maxOf(0.0, input.weightKg + delta)
            input.copy(weightKg = com.liftlog.engine.E1RMCalculator.roundTo(newWeight, increment))
        }
    }

    fun adjustReps(exerciseId: Long, delta: Int) {
        updateInput(exerciseId) { input ->
            input.copy(reps = maxOf(1, input.reps + delta))
        }
    }

    fun setType(exerciseId: Long, type: String) {
        updateInput(exerciseId) { it.copy(type = type) }
    }

    fun setRpe(exerciseId: Long, rpe: Double) {
        updateInput(exerciseId) { it.copy(rpe = rpe) }
    }

    fun toggleFlagSwung(exerciseId: Long) {
        updateInput(exerciseId) { it.copy(flagSwung = !it.flagSwung) }
    }

    fun toggleFlagPain(exerciseId: Long) {
        updateInput(exerciseId) { it.copy(flagPain = !it.flagPain) }
    }

    fun logSet(context: Context, exerciseId: Long) {
        val input = _uiState.value.prefilledInputs[exerciseId] ?: return
        val exItem = _uiState.value.exercises.find { it.exercise.id == exerciseId } ?: return

        viewModelScope.launch {
            val currentWorkingCount = exItem.loggedSets.count { it.type != "warmup" }
            val isWorkingSet = input.type != "warmup"
            val isLastPlannedSet = if (isWorkingSet) (currentWorkingCount + 1) >= exItem.plannedSets else false
            val effectiveRpe = if (isLastPlannedSet) input.rpe else null

            val (_, prResult) = workoutRepository.logSet(
                workoutId = workoutId,
                exerciseId = exerciseId,
                weightKg = input.weightKg,
                reps = input.reps,
                type = input.type,
                rpe = effectiveRpe,
                flagSwung = input.flagSwung,
                flagPain = input.flagPain,
                note = input.note
            )

            // Auto-start Rest Timer on working/drop set
            if (input.type != "warmup") {
                val restSec = computeRestSeconds(exItem)
                val timerTitle = if (isLastPlannedSet) {
                    val currentIndex = _uiState.value.exercises.indexOfFirst { it.exercise.id == exerciseId }
                    val nextEx = if (currentIndex != -1 && currentIndex + 1 < _uiState.value.exercises.size) {
                        _uiState.value.exercises[currentIndex + 1]
                    } else null
                    if (nextEx != null) "Next: ${nextEx.exercise.name}" else "${exItem.exercise.name} (Rest)"
                } else {
                    "${exItem.exercise.name} (Set ${currentWorkingCount + 2})"
                }
                RestTimerService.startTimer(context, restSec, timerTitle)
            }

            // Reload exercises and advance prefilled input to next planned set
            val refreshedExercises = workoutRepository.prepareWorkoutExercises(workoutId)
            val updatedItem = refreshedExercises.find { it.exercise.id == exerciseId }
            val newInputs = _uiState.value.prefilledInputs.toMutableMap()

            if (updatedItem != null) {
                val loggedWarmupCount = updatedItem.loggedSets.count { it.type == "warmup" }
                val warmupCount = updatedItem.warmupTargets.size
                val nextType = if (loggedWarmupCount < warmupCount) "warmup" else "working"
                val nextWeight = if (loggedWarmupCount < warmupCount) {
                    updatedItem.warmupTargets[loggedWarmupCount].weightKg
                } else {
                    updatedItem.target.targetWeightKg
                }
                val nextReps = if (loggedWarmupCount < warmupCount) {
                    updatedItem.warmupTargets[loggedWarmupCount].reps
                } else {
                    updatedItem.target.targetReps
                }

                newInputs[exerciseId] = CurrentSetInput(
                    weightKg = nextWeight,
                    reps = nextReps,
                    type = nextType,
                    rpe = 9.0
                )
            }

            _uiState.value = _uiState.value.copy(
                exercises = refreshedExercises,
                prefilledInputs = newInputs
            )
        }
    }

    private fun computeRestSeconds(item: ExerciseInWorkout): Int {
        val slot = item.slot
        if (slot?.supersetGroup != null) {
            // Check if this exercise is the first of the superset pair
            val groupSlots = _uiState.value.exercises
                .mapNotNull { it.slot }
                .filter { it.supersetGroup == slot.supersetGroup }
                .sortedBy { it.slotOrder }

            if (groupSlots.isNotEmpty() && groupSlots.first().exerciseId == item.exercise.id) {
                // First exercise of superset pair gets 30s rest!
                return 30
            }
        }
        // Second of superset or normal exercise uses normal rest
        return if (item.exercise.restSec > 0) {
            item.exercise.restSec
        } else if (item.exercise.isCompound) {
            150
        } else {
            75
        }
    }

    fun dismissPrBanner() {
        _uiState.value = _uiState.value.copy(prBanner = null)
    }

    fun updatePastSet(set: SetEntry) {
        viewModelScope.launch {
            workoutRepository.updateSet(set)
            loadWorkoutData()
        }
    }

    fun deletePastSet(setId: Long) {
        viewModelScope.launch {
            workoutRepository.deleteSet(setId)
            loadWorkoutData()
        }
    }

    fun finishWorkout(context: Context? = null, note: String?, onFinished: () -> Unit) {
        viewModelScope.launch {
            context?.let { RestTimerService.stopTimer(it) }
            workoutRepository.finishWorkout(workoutId, note)
            onFinished()
        }
    }

    fun cancelWorkout(context: Context? = null, onCancelled: () -> Unit) {
        viewModelScope.launch {
            context?.let { RestTimerService.stopTimer(it) }
            workoutRepository.cancelActiveWorkout(workoutId)
            onCancelled()
        }
    }

    class Factory(
        private val workoutId: Long,
        private val workoutRepository: WorkoutRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ActiveWorkoutViewModel(workoutId, workoutRepository, settingsRepository) as T
        }
    }
}
