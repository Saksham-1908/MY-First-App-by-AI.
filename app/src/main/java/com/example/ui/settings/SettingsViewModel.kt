package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.TimeTrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val defaultDurationMinutes: Int = 45,
    val workStartHour: Int = 8,
    val notificationsEnabled: Boolean = true,
    val showClearConfirmation: Boolean = false,
    val statusMessage: String? = null
)

class SettingsViewModel(
    private val repository: TimeTrackRepository
) : ViewModel() {

    private val _showClearConfirmation = MutableStateFlow(false)
    val showClearConfirmation: StateFlow<Boolean> = _showClearConfirmation.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.preferencesRepository.defaultDurationMinutes,
        repository.preferencesRepository.workStartHour,
        repository.preferencesRepository.notificationsEnabled,
        _showClearConfirmation,
        _statusMessage
    ) { duration, startHour, notifs, showDialog, msg ->
        SettingsUiState(
            defaultDurationMinutes = duration,
            workStartHour = startHour,
            notificationsEnabled = notifs,
            showClearConfirmation = showDialog,
            statusMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setDefaultDuration(minutes: Int) {
        viewModelScope.launch {
            repository.preferencesRepository.setDefaultDurationMinutes(minutes)
        }
    }

    fun setWorkStartHour(hour: Int) {
        viewModelScope.launch {
            repository.preferencesRepository.saveActiveTimerState(null, 0, false)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.setNotificationsEnabled(enabled)
        }
    }

    fun promptClearAllData() {
        _showClearConfirmation.value = true
    }

    fun dismissClearConfirmation() {
        _showClearConfirmation.value = false
    }

    fun confirmClearAllData() {
        viewModelScope.launch {
            repository.clearAllTasks()
            _showClearConfirmation.value = false
            _statusMessage.value = "All timetable and task data has been cleared."
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            repository.preferencesRepository.setOnboardingCompleted(false)
            _statusMessage.value = "Onboarding will be shown next time app opens."
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
