package com.playbook.ui.invite

import com.playbook.di.KmpViewModel
import com.playbook.domain.InviteDetails
import com.playbook.repository.InviteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InviteState(
    val inviteDetails: InviteDetails? = null,
    val isLoading: Boolean = false,
    val isRedeeming: Boolean = false,
    val error: String? = null,
    val isRedeemed: Boolean = false
)

class InviteViewModel(
    private val inviteRepository: InviteRepository
) : KmpViewModel() {

    private val _state = MutableStateFlow(InviteState())
    val state = _state.asStateFlow()

    fun loadInvite(token: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            inviteRepository.getInviteDetails(token).fold(
                onSuccess = { details ->
                    _state.value = _state.value.copy(
                        inviteDetails = details,
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to fetch invite details"
                    )
                }
            )
        }
    }

    fun redeemInvite(token: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRedeeming = true, error = null)
            inviteRepository.redeemInvite(token).fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        isRedeeming = false,
                        isRedeemed = true
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isRedeeming = false,
                        error = e.message ?: "Failed to redeem invite"
                    )
                }
            )
        }
    }
}
