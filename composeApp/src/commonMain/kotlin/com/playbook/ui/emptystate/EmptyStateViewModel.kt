package com.playbook.ui.emptystate

import com.playbook.di.KmpViewModel
import com.playbook.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EmptyStateUiState(
    val inviteLink: String = "",
    val profileLink: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val infoMessage: String? = null
)

class EmptyStateViewModel(
    private val authRepository: AuthRepository
) : KmpViewModel() {

    private val _state = MutableStateFlow(EmptyStateUiState())
    val state = _state.asStateFlow()

    init {
        loadProfileLink()
    }

    private fun loadProfileLink() {
        viewModelScope.launch {
            val userId = authRepository.getMe().getOrNull()?.userId
            if (userId != null) {
                _state.value = _state.value.copy(
                    profileLink = "playbook://invite/player/$userId"
                )
            }
        }
    }

    fun onInviteLinkChange(link: String) {
        _state.value = _state.value.copy(inviteLink = link, error = null)
    }

    fun onJoinTeamClick() {
        // Handled in Phase 2
        _state.value = _state.value.copy(infoMessage = "Team joining will be available in Phase 2")
    }

    fun onCreateClubClick() {
        // Handled in Phase 2
        _state.value = _state.value.copy(infoMessage = "Club creation will be available in Phase 2")
    }

    fun onProfileLinkCopied() {
        _state.value = _state.value.copy(infoMessage = "Link copied to clipboard")
    }
    
    fun dismissMessages() {
        _state.value = _state.value.copy(error = null, infoMessage = null)
    }
}
