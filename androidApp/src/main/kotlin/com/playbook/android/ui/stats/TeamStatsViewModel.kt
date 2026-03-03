package com.playbook.android.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playbook.attendance.StatsFilter
import com.playbook.attendance.aggregateStats
import com.playbook.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TeamStatsViewModel(
    private val teamId: String,
    private val attendanceRepository: AttendanceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TeamStatsScreenState())
    val state: StateFlow<TeamStatsScreenState> = _state.asStateFlow()

    init {
        load()
    }

    fun submitAction(action: TeamStatsAction) {
        when (action) {
            TeamStatsAction.Refresh -> load()
            is TeamStatsAction.FilterByEventType -> {
                _state.update { it.copy(selectedEventType = action.eventType) }
                recomputeStats()
            }
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val rows = attendanceRepository.getTeamAttendance(teamId, _state.value.from, _state.value.to)
                val filter = StatsFilter(eventType = _state.value.selectedEventType)
                val userStats = rows.groupBy { it.userId }.map { (userId, userRows) ->
                    userId to aggregateStats(userRows, filter)
                }.sortedByDescending { it.second.presencePct }
                _state.update { it.copy(rows = rows, userStats = userStats, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to load team statistics.") }
            }
        }
    }

    private fun recomputeStats() {
        val rows = _state.value.rows
        val filter = StatsFilter(eventType = _state.value.selectedEventType)
        val userStats = rows.groupBy { it.userId }.map { (userId, userRows) ->
            userId to aggregateStats(userRows, filter)
        }.sortedByDescending { it.second.presencePct }
        _state.update { it.copy(userStats = userStats) }
    }
}
