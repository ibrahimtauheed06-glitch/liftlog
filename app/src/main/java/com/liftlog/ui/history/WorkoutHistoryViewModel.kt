package com.liftlog.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liftlog.data.db.ExerciseDao
import com.liftlog.data.db.SetEntryDao
import com.liftlog.data.db.TemplateDao
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

data class PastWorkoutExerciseSummary(
    val exerciseName: String,
    val isBodyweight: Boolean,
    val workingCount: Int,
    val warmupCount: Int,
    val workingSummary: String,
    val warmupSummary: String?
)

data class PastWorkoutItem(
    val id: Long,
    val routineName: String,
    val startTs: Long,
    val endTs: Long,
    val durationMinutes: Long,
    val formattedDate: String,
    val formattedTime: String,
    val workingSets: Int,
    val warmupSets: Int,
    val totalVolumeKg: Double,
    val note: String?,
    val exerciseSummaries: List<PastWorkoutExerciseSummary>
)

data class WorkoutHistoryUiState(
    val workouts: List<PastWorkoutItem> = emptyList(),
    val isLoading: Boolean = true
)

class WorkoutHistoryViewModel(
    private val workoutRepository: WorkoutRepository,
    private val templateDao: TemplateDao,
    private val exerciseDao: ExerciseDao,
    private val setEntryDao: SetEntryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutHistoryUiState())
    val uiState: StateFlow<WorkoutHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            workoutRepository.cleanAllEmptyWorkouts()
            workoutRepository.getCompletedWorkoutsFlow().collectLatest { workouts ->
                val templates = templateDao.getAllTemplates().first().associateBy { it.id }
                val exercises = exerciseDao.getAllExercises().first().associateBy { it.id }

                val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

                val items = workouts.map { workout ->
                    val sets = setEntryDao.getSetsForWorkoutSync(workout.id)
                    val routineName = workout.templateId?.let { templates[it]?.name } ?: "Custom Workout"
                    val durationMin = maxOf(1L, ((workout.endTs ?: workout.startTs) - workout.startTs) / (1000 * 60))

                    val workingTotal = sets.count { it.type != "warmup" }
                    val warmupTotal = sets.count { it.type == "warmup" }

                    val groupedByExercise = sets.groupBy { it.exerciseId }
                    val summaries = groupedByExercise.map { (exId, exSets) ->
                        val ex = exercises[exId]
                        val exName = ex?.name ?: "Exercise"
                        val isBw = (ex?.bwFactor ?: 0.0) > 0.0

                        val working = exSets.filter { it.type != "warmup" }
                        val warmups = exSets.filter { it.type == "warmup" }

                        fun formatSetGroup(group: List<com.liftlog.data.entity.SetEntry>): String {
                            if (group.isEmpty()) return ""
                            val weights = group.map { it.weightKg }.distinct()
                            return if (weights.size == 1) {
                                val w = weights.first()
                                val repsList = group.joinToString(",") { "${it.reps}" }
                                val wStr = if (isBw) {
                                    if (w > 0.0) "BW+${if (w % 1.0 == 0.0) w.toInt() else w}kg" else "BW"
                                } else {
                                    "${if (w % 1.0 == 0.0) w.toInt() else w}kg"
                                }
                                "$wStr × $repsList"
                            } else {
                                group.joinToString(", ") { s ->
                                    val wStr = if (isBw) {
                                        if (s.weightKg > 0.0) "BW+${if (s.weightKg % 1.0 == 0.0) s.weightKg.toInt() else s.weightKg}kg" else "BW"
                                    } else {
                                        "${if (s.weightKg % 1.0 == 0.0) s.weightKg.toInt() else s.weightKg}kg"
                                    }
                                    "$wStr×${s.reps}"
                                }
                            }
                        }

                        val workingText = if (working.isNotEmpty()) {
                            "${working.size} ${if (working.size == 1) "set" else "sets"}: ${formatSetGroup(working)}"
                        } else {
                            "0 sets"
                        }

                        val warmupText = if (warmups.isNotEmpty()) {
                            "${warmups.size} warmup: ${formatSetGroup(warmups)}"
                        } else null

                        PastWorkoutExerciseSummary(
                            exerciseName = exName,
                            isBodyweight = isBw,
                            workingCount = working.size,
                            warmupCount = warmups.size,
                            workingSummary = workingText,
                            warmupSummary = warmupText
                        )
                    }

                    val totalVol = sets.filter { it.type == "working" }.sumOf { it.weightKg * it.reps }

                    PastWorkoutItem(
                        id = workout.id,
                        routineName = routineName,
                        startTs = workout.startTs,
                        endTs = workout.endTs ?: workout.startTs,
                        durationMinutes = durationMin,
                        formattedDate = dateFormat.format(Date(workout.startTs)),
                        formattedTime = timeFormat.format(Date(workout.startTs)),
                        workingSets = workingTotal,
                        warmupSets = warmupTotal,
                        totalVolumeKg = totalVol,
                        note = workout.note,
                        exerciseSummaries = summaries
                    )
                }

                _uiState.value = WorkoutHistoryUiState(
                    workouts = items,
                    isLoading = false
                )
            }
        }
    }

    fun deleteWorkout(workoutId: Long) {
        viewModelScope.launch {
            workoutRepository.discardWorkout(workoutId)
        }
    }

    class Factory(
        private val workoutRepository: WorkoutRepository,
        private val templateDao: TemplateDao,
        private val exerciseDao: ExerciseDao,
        private val setEntryDao: SetEntryDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WorkoutHistoryViewModel(workoutRepository, templateDao, exerciseDao, setEntryDao) as T
        }
    }
}
