package ch.teamorg.ui.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.teamorg.domain.InviteDetails
import ch.teamorg.repository.InviteRepository
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
) : ViewModel() {

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
                    // Handle idempotent join (already a member)
                    if (e.message?.contains("Already a member", ignoreCase = true) == true ||
                        e.message?.contains("409") == true) {
                        _state.value = _state.value.copy(
                            isRedeeming = false,
                            isRedeemed = true
                        )
                    } else {
                        _state.value = _state.value.copy(
                            isRedeeming = false,
                            error = e.message ?: "Failed to redeem invite"
                        )
                    }
                }
            )
        }
    }
}
