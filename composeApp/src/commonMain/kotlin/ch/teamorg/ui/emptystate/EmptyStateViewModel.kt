package ch.teamorg.ui.emptystate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.teamorg.repository.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EmptyStateUiState(
    val inviteLink: String = "",
    val profileLink: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val infoMessage: String? = null
)

sealed class EmptyStateEvent {
    data object NavigateToClubSetup : EmptyStateEvent()
    data class NavigateToInvite(val token: String) : EmptyStateEvent()
}

class EmptyStateViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EmptyStateUiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<EmptyStateEvent>()
    val events = _events.asSharedFlow()

    init {
        loadProfileLink()
    }

    private fun loadProfileLink() {
        viewModelScope.launch {
            authRepository.getMe().onSuccess { user ->
                _state.value = _state.value.copy(
                    profileLink = "teamorg://invite/player/${user.userId}"
                )
            }
        }
    }

    fun onInviteLinkChange(link: String) {
        _state.value = _state.value.copy(inviteLink = link, error = null)
    }

    fun onJoinTeamClick() {
        _state.value = _state.value.copy(infoMessage = "Team joining will be available in Phase 2")
        return

        val link = _state.value.inviteLink
        if (link.isBlank()) {
            _state.value = _state.value.copy(error = "Please paste an invite link")
            return
        }

        // Extract token from teamorg://invite/team/{token} or just take the whole thing if it's just the token
        val token = if (link.startsWith("teamorg://invite/team/")) {
            link.substringAfterLast("/")
        } else {
            link
        }

        if (token.isBlank()) {
            _state.value = _state.value.copy(error = "Invalid invite link")
            return
        }

        viewModelScope.launch {
            _events.emit(EmptyStateEvent.NavigateToInvite(token))
        }
    }

    fun onCreateClubClick() {
        viewModelScope.launch {
            _events.emit(EmptyStateEvent.NavigateToClubSetup)
        }
    }

    fun onProfileLinkCopied() {
        _state.value = _state.value.copy(infoMessage = "Link copied to clipboard")
    }

    fun dismissMessages() {
        _state.value = _state.value.copy(error = null, infoMessage = null)
    }
}
