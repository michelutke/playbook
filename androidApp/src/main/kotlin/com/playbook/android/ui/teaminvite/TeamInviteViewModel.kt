package com.playbook.android.ui.teaminvite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playbook.domain.CreateInviteRequest
import com.playbook.repository.InviteRepository
import com.playbook.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TeamInviteViewModel(
    private val teamId: String,
    private val inviteRepository: InviteRepository,
    private val teamRepository: TeamRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TeamInviteSheetState())
    val state: StateFlow<TeamInviteSheetState> = _state.asStateFlow()

    init {
        load()
    }

    fun submitAction(action: TeamInviteAction) {
        when (action) {
            is TeamInviteAction.EmailChanged -> _state.update { it.copy(email = action.email, error = null) }
            is TeamInviteAction.RoleChanged -> _state.update { it.copy(role = action.role) }
            is TeamInviteAction.Send -> send()
            is TeamInviteAction.CopyLink -> { /* clipboard handled in UI */ }
            is TeamInviteAction.Revoke -> revoke(action.inviteId)
        }
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val team = teamRepository.getById(teamId)
                val invites = inviteRepository.listPending(teamId)
                _state.update {
                    it.copy(
                        teamName = team?.name ?: "",
                        pendingInvites = invites,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to load invites.") }
            }
        }
    }

    private fun send() {
        val current = _state.value
        if (current.email.isBlank()) return
        _state.update { it.copy(isSending = true, error = null) }
        viewModelScope.launch {
            try {
                inviteRepository.create(
                    teamId,
                    CreateInviteRequest(email = current.email.trim(), role = current.role),
                    invitedByUserId = "TODO_user_id",
                )
                val invites = inviteRepository.listPending(teamId)
                _state.update { it.copy(email = "", pendingInvites = invites, isSending = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to send invite.", isSending = false) }
            }
        }
    }

    private fun revoke(inviteId: String) {
        viewModelScope.launch {
            try {
                inviteRepository.revoke(inviteId)
                val invites = inviteRepository.listPending(teamId)
                _state.update { it.copy(pendingInvites = invites) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to revoke invite.") }
            }
        }
    }
}
