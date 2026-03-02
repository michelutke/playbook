package com.playbook.android.ui.clubcoachinvite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playbook.domain.CreateCoachLinkRequest
import com.playbook.domain.CreateInviteRequest
import com.playbook.domain.MemberRole
import com.playbook.repository.ClubRepository
import com.playbook.repository.CoachLinkRepository
import com.playbook.repository.InviteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ClubCoachInviteViewModel(
    private val clubId: String,
    private val coachLinkRepository: CoachLinkRepository,
    private val inviteRepository: InviteRepository,
    private val clubRepository: ClubRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ClubCoachInviteSheetState())
    val state: StateFlow<ClubCoachInviteSheetState> = _state.asStateFlow()

    init {
        load()
    }

    fun submitAction(action: ClubCoachInviteAction) {
        when (action) {
            is ClubCoachInviteAction.EmailChanged -> _state.update { it.copy(email = action.email, error = null) }
            is ClubCoachInviteAction.Send -> send()
            is ClubCoachInviteAction.CopyLink -> _state.update { it.copy(isCopied = true) }
            is ClubCoachInviteAction.RotateLink -> rotateLink()
        }
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val club = clubRepository.getById(clubId)
                val link = coachLinkRepository.getActive(clubId)
                _state.update {
                    it.copy(
                        clubName = club?.name ?: "",
                        coachLink = link,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to load coach link.") }
            }
        }
    }

    private fun send() {
        val current = _state.value
        if (current.email.isBlank()) return
        _state.update { it.copy(isSending = true, error = null) }
        viewModelScope.launch {
            try {
                val teamId = "" // coach invite is club-level, no specific team
                inviteRepository.create(
                    teamId,
                    CreateInviteRequest(email = current.email.trim(), role = MemberRole.COACH),
                    invitedByUserId = "TODO_user_id",
                )
                _state.update { it.copy(email = "", isSending = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to send invite.", isSending = false) }
            }
        }
    }

    private fun rotateLink() {
        viewModelScope.launch {
            try {
                val newLink = coachLinkRepository.rotate(
                    clubId,
                    CreateCoachLinkRequest(expiresInDays = 7),
                    createdByUserId = "TODO_user_id",
                )
                _state.update { it.copy(coachLink = newLink) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to rotate link.") }
            }
        }
    }
}
