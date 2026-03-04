package com.playbook.ui.teamdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playbook.domain.MemberRole
import com.playbook.domain.RosterMember
import com.playbook.repository.MembershipRepository
import com.playbook.repository.TeamRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class TeamDetailEvent {
    data object NavigateBack : TeamDetailEvent()
}

class TeamDetailViewModel(
    private val teamId: String,
    private val clubId: String,
    private val membershipRepository: MembershipRepository,
    private val teamRepository: TeamRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TeamDetailScreenState())
    val state: StateFlow<TeamDetailScreenState> = _state.asStateFlow()

    private val _events = Channel<TeamDetailEvent>()
    val events = _events.receiveAsFlow()

    private var allRoster: List<RosterMember> = emptyList()

    init {
        load()
    }

    fun submitAction(action: TeamDetailAction) {
        when (action) {
            is TeamDetailAction.Refresh -> load()
            is TeamDetailAction.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = action.query) }
                applyFilter()
            }
            is TeamDetailAction.TabSelected -> _state.update { it.copy(selectedTab = action.tab) }
            is TeamDetailAction.RemoveMember -> removeMember(action.userId)
            is TeamDetailAction.LeaveTeam -> leaveTeam()
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val team = teamRepository.getById(teamId)
                val roster = membershipRepository.getRoster(teamId)
                allRoster = roster
                val coaches = roster.filter { it.roles.contains(MemberRole.COACH) }
                val players = roster.filter { it.roles.contains(MemberRole.PLAYER) }
                _state.update {
                    it.copy(
                        team = team,
                        coaches = coaches,
                        players = players,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to load team details.") }
            }
        }
    }

    private fun applyFilter() {
        val query = _state.value.searchQuery.trim().lowercase()
        if (query.isEmpty()) {
            val coaches = allRoster.filter { it.roles.contains(MemberRole.COACH) }
            val players = allRoster.filter { it.roles.contains(MemberRole.PLAYER) }
            _state.update { it.copy(coaches = coaches, players = players) }
        } else {
            val coaches = allRoster.filter {
                it.roles.contains(MemberRole.COACH) &&
                    it.displayName?.lowercase()?.contains(query) == true
            }
            val players = allRoster.filter {
                it.roles.contains(MemberRole.PLAYER) &&
                    it.displayName?.lowercase()?.contains(query) == true
            }
            _state.update { it.copy(coaches = coaches, players = players) }
        }
    }

    private fun removeMember(userId: String) {
        viewModelScope.launch {
            try {
                membershipRepository.removeMember(teamId, userId)
                load()
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to remove member.") }
            }
        }
    }

    private fun leaveTeam() {
        viewModelScope.launch {
            try {
                membershipRepository.leaveTeam(teamId, "TODO_user_id")
                _events.send(TeamDetailEvent.NavigateBack)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to leave team.") }
            }
        }
    }
}
