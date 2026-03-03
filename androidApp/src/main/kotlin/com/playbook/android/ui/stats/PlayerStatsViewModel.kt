package com.playbook.android.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playbook.attendance.StatsFilter
import com.playbook.attendance.aggregateStats
import com.playbook.domain.EventType
import com.playbook.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerStatsViewModel(
    private val userId: String,
    private val teamId: String?,
    private val attendanceRepository: AttendanceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerStatsScreenState())
    val state: StateFlow<PlayerStatsScreenState> = _state.asStateFlow()

    init {
        load()
    }

    fun submitAction(action: PlayerStatsAction) {
        when (action) {
            PlayerStatsAction.Refresh -> load()
            is PlayerStatsAction.FilterByEventType -> {
                _state.update { it.copy(selectedEventType = action.eventType) }
                recomputeStats()
            }
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val rows = attendanceRepository.getUserAttendance(userId, _state.value.from, _state.value.to)
                val stats = aggregateStats(rows, StatsFilter(eventType = _state.value.selectedEventType))
                _state.update { it.copy(rows = rows, stats = stats, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to load statistics.") }
            }
        }
    }

    private fun recomputeStats() {
        val rows = _state.value.rows
        val stats = aggregateStats(rows, StatsFilter(eventType = _state.value.selectedEventType))
        _state.update { it.copy(stats = stats) }
    }
}
