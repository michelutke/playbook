package com.playbook.ui.team

import com.playbook.di.KmpViewModel
import com.playbook.domain.TeamMember
import com.playbook.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TeamRosterState(
    val members: List<TeamMember> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val inviteUrl: String? = null
)

class TeamRosterViewModel(
    private val teamRepository: TeamRepository
) : KmpViewModel() {

    private val _state = MutableStateFlow(TeamRosterState())
    val state = _state.asStateFlow()

    fun loadRoster(teamId: String, isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _state.value = _state.value.copy(isRefreshing = true)
            } else {
                _state.value = _state.value.copy(isLoading = true, error = null)
            }

            teamRepository.getTeamRoster(teamId).fold(
                onSuccess = { members ->
                    _state.value = _state.value.copy(
                        members = members,
                        isLoading = false,
                        isRefreshing = false
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.message ?: "Failed to fetch roster"
                    )
                }
            )
        }
    }

    fun removeMember(teamId: String, userId: String) {
        viewModelScope.launch {
            teamRepository.removeMember(teamId, userId).fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        members = _state.value.members.filterNot { it.userId == userId }
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(error = e.message ?: "Failed to remove member")
                }
            )
        }
    }

    fun createInvite(teamId: String, role: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(error = null)
            teamRepository.createInvite(teamId, role, null).fold(
                onSuccess = { url ->
                    _state.value = _state.value.copy(inviteUrl = url)
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(error = e.message ?: "Failed to create invite")
                }
            )
        }
    }

    fun resetInvite() {
        _state.value = _state.value.copy(inviteUrl = null)
    }
}
