package com.playbook.ui.eventdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playbook.attendance.isDeadlinePassed
import com.playbook.domain.AttendanceResponseStatus
import com.playbook.domain.CancelEventRequest
import com.playbook.domain.RecurringScope
import com.playbook.domain.UpdateAttendanceRequest
import com.playbook.repository.AttendanceRepository
import com.playbook.repository.EventRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class EventDetailNavEvent {
    data class NavigateToEdit(val eventId: String, val scope: RecurringScope) : EventDetailNavEvent()
    data class NavigateToDuplicate(val eventId: String) : EventDetailNavEvent()
    data object NavigateBack : EventDetailNavEvent()
}

class EventDetailViewModel(
    private val eventId: String,
    private val eventRepository: EventRepository,
    private val attendanceRepository: AttendanceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EventDetailScreenState())
    val state: StateFlow<EventDetailScreenState> = _state.asStateFlow()

    private val _attendanceState = MutableStateFlow(AttendanceCardState())
    val attendanceState: StateFlow<AttendanceCardState> = _attendanceState.asStateFlow()

    private val _navEvents = Channel<EventDetailNavEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    init {
        load()
    }

    fun submitAction(action: EventDetailAction) {
        when (action) {
            EventDetailAction.Refresh -> load()
            EventDetailAction.EditRequested -> {
                val event = _state.value.event ?: return
                if (event.seriesId != null) {
                    _state.update { it.copy(showScopeSheet = true, pendingAction = PendingEventAction.EDIT) }
                } else {
                    viewModelScope.launch {
                        _navEvents.send(EventDetailNavEvent.NavigateToEdit(eventId, RecurringScope.THIS_ONLY))
                    }
                }
            }
            EventDetailAction.DuplicateRequested ->
                viewModelScope.launch { _navEvents.send(EventDetailNavEvent.NavigateToDuplicate(eventId)) }
            EventDetailAction.CancelRequested -> {
                val event = _state.value.event ?: return
                if (event.seriesId != null) {
                    _state.update { it.copy(showScopeSheet = true, pendingAction = PendingEventAction.CANCEL) }
                } else {
                    _state.update { it.copy(showCancelDialog = true) }
                }
            }
            is EventDetailAction.ScopeSelectedForEdit -> {
                _state.update { it.copy(showScopeSheet = false, pendingAction = null) }
                viewModelScope.launch { _navEvents.send(EventDetailNavEvent.NavigateToEdit(eventId, action.scope)) }
            }
            is EventDetailAction.ScopeSelectedForCancel -> {
                _state.update { it.copy(showScopeSheet = false, pendingAction = null, showCancelDialog = true, pendingCancelScope = action.scope) }
            }
            EventDetailAction.DismissScopeSheet ->
                _state.update { it.copy(showScopeSheet = false, pendingAction = null) }
            EventDetailAction.ConfirmCancel -> cancelEvent(_state.value.pendingCancelScope)
            EventDetailAction.DismissCancelDialog -> _state.update { it.copy(showCancelDialog = false) }
            is EventDetailAction.AttendanceResponseTapped -> handleAttendanceTap(action.status)
            is EventDetailAction.BegrundungSubmitted -> submitAttendance(
                status = _attendanceState.value.pendingStatus ?: return,
                reason = action.reason,
            )
            EventDetailAction.BegrundungDismissed ->
                _attendanceState.update { it.copy(showBegrundungSheet = false, pendingStatus = null) }
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val event = eventRepository.getById(eventId)
                _state.update { it.copy(event = event, isLoading = false) }
                _attendanceState.update { it.copy(isDeadlinePassed = event?.let { e -> isDeadlinePassed(e) } ?: false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to load event.") }
            }
        }
        loadAttendance()
    }

    private fun loadAttendance() {
        _attendanceState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val response = attendanceRepository.getMyAttendance(eventId, "")
                _attendanceState.update { it.copy(myResponse = response, isLoading = false) }
            } catch (e: Exception) {
                _attendanceState.update { it.copy(isLoading = false, error = "Failed to load attendance.") }
            }
        }
    }

    private fun handleAttendanceTap(status: AttendanceResponseStatus) {
        when (status) {
            AttendanceResponseStatus.CONFIRMED -> submitAttendance(status, null)
            AttendanceResponseStatus.DECLINED, AttendanceResponseStatus.UNSURE ->
                _attendanceState.update { it.copy(showBegrundungSheet = true, pendingStatus = status) }
            else -> submitAttendance(status, null)
        }
    }

    private fun submitAttendance(status: AttendanceResponseStatus, reason: String?) {
        _attendanceState.update { it.copy(isLoading = true, showBegrundungSheet = false, pendingStatus = null) }
        viewModelScope.launch {
            try {
                val response = attendanceRepository.upsertAttendance(
                    eventId,
                    "",
                    UpdateAttendanceRequest(status, reason),
                )
                _attendanceState.update { it.copy(myResponse = response, isLoading = false) }
            } catch (e: Exception) {
                _attendanceState.update { it.copy(isLoading = false, error = "Failed to update attendance.") }
            }
        }
    }

    private fun cancelEvent(scope: RecurringScope) {
        viewModelScope.launch {
            try {
                eventRepository.cancel(eventId, CancelEventRequest(scope))
                _state.update { it.copy(showCancelDialog = false) }
                _navEvents.send(EventDetailNavEvent.NavigateBack)
            } catch (e: Exception) {
                _state.update { it.copy(showCancelDialog = false, error = "Failed to cancel event.") }
            }
        }
    }
}
