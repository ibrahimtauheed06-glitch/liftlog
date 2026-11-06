package com.liftlog.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liftlog.data.db.ExerciseDao
import com.liftlog.data.db.SetEntryDao
import com.liftlog.data.db.TemplateDao
import com.liftlog.data.entity.Exercise
import com.liftlog.data.entity.SetEntry
import com.liftlog.data.entity.Workout
import com.liftlog.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExerciseDetailGroup(
    val exercise: Exercise,
    val sets: List<SetEntry>
)

data class WorkoutDetailUiState(
    val workout: Workout? = null,
    val routineName: String = "",
    val formattedDate: String = "",
    val formattedTime: String = "",
    val durationMinutes: Long = 0,
    val note: String = "",
    val exercises: List<ExerciseDetailGroup> = emptyList(),
    val allAvailableExercises: List<Exercise> = emptyList(),
    val totalVolumeKg: Double = 0.0,
    val totalSets: Int = 0,
    val workingSets: Int = 0,
    val warmupSets: Int = 0,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false
)

class WorkoutDetailViewModel(
    val workoutId: Long,
    private val workoutRepository: WorkoutRepository,
    private val templateDao: TemplateDao,
    private val exerciseDao: ExerciseDao,
    private val setEntryDao: SetEntryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutDetailUiState())
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState.asStateFlow()

    init {
        loadWorkoutData()
    }

    private fun loadWorkoutData() {
        viewModelScope.launch {
            val workout = workoutRepository.getWorkoutById(workoutId)
            if (workout == null || workout.isDiscarded) {
                _uiState.value = _uiState.value.copy(isLoading = false, isDeleted = true)
                return@launch
            }

            val template = workout.templateId?.let { templateDao.getTemplateById(it) }
            val routineName = template?.name ?: "Custom Workout"
            val allExList = exerciseDao.getAllExercises().first().filter { !it.archived }

            val dateFormat = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val durationMin = maxOf(1L, ((workout.endTs ?: workout.startTs) - workout.startTs) / (1000 * 60))

            // Reactively observe sets for this workout
            setEntryDao.getSetsForWorkout(workoutId).collectLatest { sets ->
                val exercisesMap = exerciseDao.getAllExercises().first().associateBy { it.id }
                val grouped = sets.groupBy { it.exerciseId }.mapNotNull { (exId, exSets) ->
                    val ex = exercisesMap[exId] ?: return@mapNotNull null
                    ExerciseDetailGroup(exercise = ex, sets = exSets.sortedBy { it.ts })
                }

                val totalVol = sets.filter { it.type == "working" }.sumOf { it.weightKg * it.reps }
                val workingCount = sets.count { it.type != "warmup" }
                val warmupCount = sets.count { it.type == "warmup" }

                _uiState.value = WorkoutDetailUiState(
                    workout = workout,
                    routineName = routineName,
                    formattedDate = dateFormat.format(Date(workout.startTs)),
                    formattedTime = timeFormat.format(Date(workout.startTs)),
                    durationMinutes = durationMin,
                    note = workout.note ?: "",
                    exercises = grouped,
                    allAvailableExercises = allExList,
                    totalVolumeKg = totalVol,
                    totalSets = sets.size,
                    workingSets = workingCount,
                    warmupSets = warmupCount,
                    isLoading = false
                )
            }
        }
    }

    fun updateSet(set: SetEntry) {
        viewModelScope.launch {
            workoutRepository.updateSet(set)
        }
    }

    fun updateSetWeightAndReps(set: SetEntry, newWeight: Double, newReps: Int) {
        viewModelScope.launch {
            workoutRepository.updateSet(set.copy(weightKg = newWeight, reps = newReps))
        }
    }

    fun toggleSetType(set: SetEntry) {
        viewModelScope.launch {
            val newType = if (set.type == "working") "warmup" else "working"
            workoutRepository.updateSet(set.copy(type = newType))
        }
    }

    fun deleteSet(setId: Long) {
        viewModelScope.launch {
            workoutRepository.deleteSet(setId)
        }
    }

    fun addSet(exerciseId: Long) {
        viewModelScope.launch {
            val currentGroup = _uiState.value.exercises.find { it.exercise.id == exerciseId }
            val lastSet = currentGroup?.sets?.lastOrNull()
            val ex = exerciseDao.getExerciseById(exerciseId) ?: return@launch

            val weight = lastSet?.weightKg ?: if (ex.defaultWeightKg > 0.0) ex.defaultWeightKg else 20.0
            val reps = lastSet?.reps ?: ex.repLo
            val now = System.currentTimeMillis()

            val newSet = SetEntry(
                workoutId = workoutId,
                exerciseId = exerciseId,
                ts = now,
                weightKg = weight,
                reps = reps,
                type = "working",
                rpe = null,
                flagSwung = false,
                flagPain = false,
                note = null
            )
            setEntryDao.insertSet(newSet)
        }
    }

    fun addExerciseToWorkout(exerciseId: Long) {
        viewModelScope.launch {
            val ex = exerciseDao.getExerciseById(exerciseId) ?: return@launch
            val now = System.currentTimeMillis()
            val weight = if (ex.defaultWeightKg > 0.0) ex.defaultWeightKg else 20.0

            val defaultSet = SetEntry(
                workoutId = workoutId,
                exerciseId = exerciseId,
                ts = now,
                weightKg = weight,
                reps = ex.repLo,
                type = "working",
                rpe = null,
                flagSwung = false,
                flagPain = false,
                note = null
            )
            setEntryDao.insertSet(defaultSet)
        }
    }

    fun removeExercise(exerciseId: Long) {
        viewModelScope.launch {
            workoutRepository.deleteExerciseFromWorkout(workoutId, exerciseId)
        }
    }

    fun updateWorkoutNote(note: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(note = note)
            workoutRepository.updateWorkoutNote(workoutId, note.ifBlank { null })
        }
    }

    fun deleteWorkout(onDeleted: () -> Unit) {
        viewModelScope.launch {
            workoutRepository.discardWorkout(workoutId)
            onDeleted()
        }
    }

    class Factory(
        private val workoutId: Long,
        private val workoutRepository: WorkoutRepository,
        private val templateDao: TemplateDao,
        private val exerciseDao: ExerciseDao,
        private val setEntryDao: SetEntryDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WorkoutDetailViewModel(workoutId, workoutRepository, templateDao, exerciseDao, setEntryDao) as T
        }
    }
}
