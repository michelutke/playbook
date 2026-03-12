package com.playbook.ui.playerprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playbook.domain.MemberRole
import com.playbook.repository.MembershipRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class PlayerProfileEvent {
    data object NavigateBack : PlayerProfileEvent()
}

class PlayerProfileViewModel(
    private val teamId: String,
    private val userId: String,
    private val membershipRepository: MembershipRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerProfileScreenState())
    val state: StateFlow<PlayerProfileScreenState> = _state.asStateFlow()

    private val _events = Channel<PlayerProfileEvent>()
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun submitAction(action: PlayerProfileAction) {
        when (action) {
            is PlayerProfileAction.Refresh -> load()
            is PlayerProfileAction.AddCoachRole -> addCoachRole()
            is PlayerProfileAction.RemoveCoachRole -> removeCoachRole()
            is PlayerProfileAction.RemoveFromTeam -> removeFromTeam()
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val roster = membershipRepository.getRoster(teamId)
                val member = roster.find { it.userId == userId }
                val profile = membershipRepository.getProfile(teamId, userId)
                _state.update { it.copy(member = member, profile = profile, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to load profile.") }
            }
        }
    }

    private fun addCoachRole() {
        viewModelScope.launch {
            try {
                membershipRepository.addRole(teamId, userId, MemberRole.COACH, "TODO_user_id")
                load()
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to add coach role.") }
            }
        }
    }

    private fun removeCoachRole() {
        viewModelScope.launch {
            try {
                membershipRepository.removeRole(teamId, userId, MemberRole.COACH)
                load()
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to remove coach role.") }
            }
        }
    }

    private fun removeFromTeam() {
        viewModelScope.launch {
            try {
                membershipRepository.removeMember(teamId, userId)
                _events.send(PlayerProfileEvent.NavigateBack)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to remove member.") }
            }
        }
    }
}
