package com.liftlog.ui.train

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liftlog.data.db.TemplateDao
import com.liftlog.data.entity.Template
import com.liftlog.data.entity.Workout
import com.liftlog.data.repository.BackupRepository
import com.liftlog.data.repository.SettingsRepository
import com.liftlog.data.repository.WorkoutRepository
import com.liftlog.engine.DeloadDetector
import com.liftlog.engine.SessionData
import com.liftlog.engine.SetData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class TrainUiState(
    val activeWorkout: Workout? = null,
    val templates: List<Template> = emptyList(),
    val suggestedTemplateId: Long? = null,
    val isTodayRestDay: Boolean = false,
    val todayScheduledRoutineName: String = "",
    val dayOfWeekName: String = "",
    val isTodayWorkoutCompleted: Boolean = false,
    val completedTodayWorkoutName: String? = null,
    val showDeloadBanner: Boolean = false,
    val showBackupReminder: Boolean = false,
    val isLoading: Boolean = false
)

class TrainViewModel(
    private val workoutRepository: WorkoutRepository,
    private val templateDao: TemplateDao,
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _showDeloadBanner = MutableStateFlow(false)

    private val completedAndSettingsFlow = combine(
        workoutRepository.getCompletedWorkoutsFlow(),
        settingsRepository.settingsFlow
    ) { completed, settings -> Pair(completed, settings) }

    val uiState: StateFlow<TrainUiState> = combine(
        workoutRepository.getActiveWorkoutFlow(),
        templateDao.getAllTemplates(),
        backupRepository.isBackupOverdueFlow(),
        completedAndSettingsFlow,
        _showDeloadBanner
    ) { active, templates, isOverdue, (completedWorkouts, settings), showDeload ->
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dayName = when (dayOfWeek) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> "Today"
        }

        val (routineName, isRest) = when (dayOfWeek) {
            Calendar.MONDAY -> "Upper A" to false
            Calendar.TUESDAY -> "Lower A" to false
            Calendar.WEDNESDAY -> "Rest Day" to true
            Calendar.THURSDAY -> "Upper B" to false
            Calendar.FRIDAY -> "Lower B" to false
            Calendar.SATURDAY -> "Rest Day" to true
            Calendar.SUNDAY -> "Rest Day" to true
            else -> "Upper A" to false
        }

        val matchedTemplateId = if (!isRest) {
            templates.find { it.name.equals(routineName, ignoreCase = true) }?.id
        } else null

        // Check if workout completed in today's rollover window
        val (startOfToday, endOfToday) = com.liftlog.data.repository.ProteinRepository.getDayWindow(
            System.currentTimeMillis(),
            settings.dayRolloverHour
        )
        val completedToday = completedWorkouts.filter { it.endTs != null && it.endTs in startOfToday..endOfToday }
        val isCompletedToday = completedToday.isNotEmpty()
        val lastCompleted = completedToday.maxByOrNull { it.endTs ?: 0L }
        val completedName = lastCompleted?.let { w ->
            templates.find { it.id == w.templateId }?.name ?: "Workout"
        }

        TrainUiState(
            activeWorkout = active,
            templates = templates,
            suggestedTemplateId = matchedTemplateId,
            isTodayRestDay = isRest,
            todayScheduledRoutineName = routineName,
            dayOfWeekName = dayName,
            isTodayWorkoutCompleted = isCompletedToday,
            completedTodayWorkoutName = completedName,
            showDeloadBanner = showDeload,
            showBackupReminder = isOverdue,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrainUiState(isLoading = true))

    init {
        viewModelScope.launch {
            workoutRepository.cleanEmptyUnfinishedWorkouts()
        }
    }

    fun startWorkout(templateId: Long?, onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            val workoutId = workoutRepository.startWorkout(templateId)
            onStarted(workoutId)
        }
    }

    fun dismissDeload() {
        viewModelScope.launch {
            settingsRepository.dismissDeload()
            _showDeloadBanner.value = false
        }
    }

    fun acceptDeload() {
        viewModelScope.launch {
            settingsRepository.acceptDeload()
            _showDeloadBanner.value = false
        }
    }

    class Factory(
        private val workoutRepository: WorkoutRepository,
        private val templateDao: TemplateDao,
        private val settingsRepository: SettingsRepository,
        private val backupRepository: BackupRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TrainViewModel(workoutRepository, templateDao, settingsRepository, backupRepository) as T
        }
    }
}
