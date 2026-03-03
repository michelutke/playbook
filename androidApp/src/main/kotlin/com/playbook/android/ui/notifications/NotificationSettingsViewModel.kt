package com.playbook.android.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playbook.domain.NotificationSettings
import com.playbook.domain.UpdateNotificationSettingsRequest
import com.playbook.repository.NotificationSettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationSettingsState(
    val settings: NotificationSettings? = null,
    val isCoach: Boolean = false, // TODO: wire to user session role once auth module exists
    val isLoading: Boolean = false,
    val pushPermissionDenied: Boolean = false,
    val error: String? = null,
)

sealed class NotificationSettingsAction {
    object Load : NotificationSettingsAction()
    data class SetNewEvents(val enabled: Boolean) : NotificationSettingsAction()
    data class SetEventChanges(val enabled: Boolean) : NotificationSettingsAction()
    data class SetEventCancellations(val enabled: Boolean) : NotificationSettingsAction()
    data class SetReminders(val enabled: Boolean) : NotificationSettingsAction()
    data class SetReminderLeadTime(val value: String) : NotificationSettingsAction()
    data class SetAttendancePerResponse(val enabled: Boolean) : NotificationSettingsAction()
    data class SetAttendanceSummary(val enabled: Boolean) : NotificationSettingsAction()
    data class SetAttendanceSummaryLeadTime(val value: String) : NotificationSettingsAction()
    data class SetAbwesenheitChanges(val enabled: Boolean) : NotificationSettingsAction()
}

class NotificationSettingsViewModel(
    private val repository: NotificationSettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationSettingsState())
    val state: StateFlow<NotificationSettingsState> = _state.asStateFlow()

    private var saveJob: Job? = null

    init {
        submitAction(NotificationSettingsAction.Load)
    }

    fun submitAction(action: NotificationSettingsAction) {
        when (action) {
            is NotificationSettingsAction.Load -> load()
            is NotificationSettingsAction.SetNewEvents ->
                applyChange { it.copy(newEvents = action.enabled) }
            is NotificationSettingsAction.SetEventChanges ->
                applyChange { it.copy(eventChanges = action.enabled) }
            is NotificationSettingsAction.SetEventCancellations ->
                applyChange { it.copy(eventCancellations = action.enabled) }
            is NotificationSettingsAction.SetReminders ->
                applyChange { it.copy(reminders = action.enabled) }
            is NotificationSettingsAction.SetReminderLeadTime ->
                applyChange { it.copy(reminderLeadTime = action.value) }
            is NotificationSettingsAction.SetAttendancePerResponse ->
                applyChange { it.copy(attendancePerResponse = action.enabled) }
            is NotificationSettingsAction.SetAttendanceSummary ->
                applyChange { it.copy(attendanceSummary = action.enabled) }
            is NotificationSettingsAction.SetAttendanceSummaryLeadTime ->
                applyChange { it.copy(attendanceSummaryLeadTime = action.value) }
            is NotificationSettingsAction.SetAbwesenheitChanges ->
                applyChange { it.copy(abwesenheitChanges = action.enabled) }
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val settings = repository.getSettings()
                _state.update { it.copy(settings = settings, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to load settings") }
            }
        }
    }

    private fun applyChange(transform: (NotificationSettings) -> NotificationSettings) {
        val current = _state.value.settings ?: return
        val updated = transform(current)
        _state.update { it.copy(settings = updated) }
        scheduleSave(updated)
    }

    private fun scheduleSave(settings: NotificationSettings) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            try {
                repository.updateSettings(
                    UpdateNotificationSettingsRequest(
                        newEvents = settings.newEvents,
                        eventChanges = settings.eventChanges,
                        eventCancellations = settings.eventCancellations,
                        reminders = settings.reminders,
                        reminderLeadTime = settings.reminderLeadTime,
                        attendancePerResponse = settings.attendancePerResponse,
                        attendanceSummary = settings.attendanceSummary,
                        attendanceSummaryLeadTime = settings.attendanceSummaryLeadTime,
                        abwesenheitChanges = settings.abwesenheitChanges,
                    )
                )
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to save settings") }
            }
        }
    }
}
