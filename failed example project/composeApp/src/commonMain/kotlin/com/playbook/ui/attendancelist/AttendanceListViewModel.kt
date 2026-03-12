package com.playbook.ui.attendancelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playbook.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AttendanceListViewModel(
    private val eventId: String,
    private val attendanceRepository: AttendanceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AttendanceListScreenState())
    val state: StateFlow<AttendanceListScreenState> = _state.asStateFlow()

    init {
        load()
    }

    fun submitAction(action: AttendanceListAction) {
        when (action) {
            AttendanceListAction.Refresh -> load()
            is AttendanceListAction.ToggleExpand -> {
                val current = _state.value.expandedUserId
                _state.update { it.copy(expandedUserId = if (current == action.userId) null else action.userId) }
            }
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val teamView = attendanceRepository.getEventAttendance(eventId)
                _state.update { it.copy(teamView = teamView, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to load attendance.") }
            }
        }
    }
}
