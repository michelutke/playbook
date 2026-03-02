package com.playbook.android.ui.inviteaccept

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playbook.repository.InviteRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class InviteAcceptEvent {
    data class Accepted(val clubId: String) : InviteAcceptEvent()
    data object Declined : InviteAcceptEvent()
}

class InviteAcceptViewModel(
    private val token: String,
    private val inviteRepository: InviteRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(InviteAcceptScreenState())
    val state: StateFlow<InviteAcceptScreenState> = _state.asStateFlow()

    private val _events = Channel<InviteAcceptEvent>()
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun submitAction(action: InviteAcceptAction) {
        when (action) {
            is InviteAcceptAction.Accept -> accept()
            is InviteAcceptAction.Decline -> decline()
        }
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val context = inviteRepository.resolveToken(token)
                _state.update { it.copy(context = context, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to load invite.") }
            }
        }
    }

    private fun accept() {
        _state.update { it.copy(isAccepting = true, error = null) }
        viewModelScope.launch {
            try {
                inviteRepository.accept(token, "TODO_user_id")
                val clubId = _state.value.context?.invite?.teamId ?: ""
                _events.send(InviteAcceptEvent.Accepted(clubId))
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to accept invite.", isAccepting = false) }
            }
        }
    }

    private fun decline() {
        viewModelScope.launch {
            _events.send(InviteAcceptEvent.Declined)
        }
    }
}
