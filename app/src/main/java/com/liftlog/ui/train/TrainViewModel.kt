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

    private val _suggestedTemplateId = MutableStateFlow<Long?>(null)
    private val _showDeloadBanner = MutableStateFlow(false)
    private val _showBackupReminder = MutableStateFlow(false)

    val uiState: StateFlow<TrainUiState> = combine(
        workoutRepository.getActiveWorkoutFlow(),
        templateDao.getAllTemplates(),
        _suggestedTemplateId,
        _showDeloadBanner,
        _showBackupReminder
    ) { active, templates, suggestedId, showDeload, showBackup ->
        TrainUiState(
            activeWorkout = active,
            templates = templates,
            suggestedTemplateId = suggestedId ?: determineSuggestedTemplateId(templates),
            showDeloadBanner = showDeload,
            showBackupReminder = showBackup,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrainUiState(isLoading = true))

    init {
        checkDeloadAndBackup()
    }

    private fun checkDeloadAndBackup() {
        viewModelScope.launch {
            _showBackupReminder.value = backupRepository.isBackupOverdue()

            // Check deload
            val settings = settingsRepository.settingsFlow.stateIn(viewModelScope).value
            val isAlreadyDeloading = settings.deloadActiveUntilTs > System.currentTimeMillis()
            if (!isAlreadyDeloading) {
                val allExercises = workoutRepository.getActiveWorkout() // check history
                // We'll evaluate deload suggestion
            }
        }
    }

    private fun determineSuggestedTemplateId(templates: List<Template>): Long? {
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        // Mon = 2 -> Upper A, Tue = 3 -> Lower A, Thu = 5 -> Upper B, Fri = 6 -> Lower B
        val targetName = when (dayOfWeek) {
            Calendar.MONDAY -> "Upper A"
            Calendar.TUESDAY -> "Lower A"
            Calendar.THURSDAY -> "Upper B"
            Calendar.FRIDAY -> "Lower B"
            else -> "Upper A"
        }
        return templates.find { it.name.equals(targetName, ignoreCase = true) }?.id
            ?: templates.firstOrNull()?.id
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
