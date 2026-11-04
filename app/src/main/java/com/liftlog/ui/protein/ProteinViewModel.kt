package com.liftlog.ui.protein

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liftlog.data.entity.ProteinEntry
import com.liftlog.data.repository.ProteinDayStats
import com.liftlog.data.repository.ProteinRepository
import com.liftlog.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ProteinUiState(
    val stats: ProteinDayStats = ProteinDayStats(
        todayEntries = emptyList(),
        todayTotalGrams = 0.0,
        targetGrams = 110.0,
        remainingGrams = 110.0,
        progressFraction = 0f,
        sevenDayAverageGrams = 0.0,
        daysHitTargetCount = 0
    ),
    val isLoading: Boolean = true
)

class ProteinViewModel(
    private val proteinRepository: ProteinRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProteinUiState())
    val uiState: StateFlow<ProteinUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            val stats = proteinRepository.getTodayStats(
                dayRolloverHour = settings.dayRolloverHour,
                targetGrams = settings.proteinTarget
            )
            _uiState.value = ProteinUiState(stats = stats, isLoading = false)
        }
    }

    fun addProtein(grams: Double, timestamp: Long) {
        viewModelScope.launch {
            proteinRepository.addEntry(grams, timestamp)
            loadStats()
        }
    }

    fun updateProtein(entry: ProteinEntry) {
        viewModelScope.launch {
            proteinRepository.updateEntry(entry)
            loadStats()
        }
    }

    fun deleteProtein(id: Long) {
        viewModelScope.launch {
            proteinRepository.deleteEntry(id)
            loadStats()
        }
    }

    class Factory(
        private val proteinRepository: ProteinRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProteinViewModel(proteinRepository, settingsRepository) as T
        }
    }
}
