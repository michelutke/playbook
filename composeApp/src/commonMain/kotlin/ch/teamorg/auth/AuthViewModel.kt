package ch.teamorg.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.teamorg.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        checkAuthState()
    }

    fun checkAuthState() {
        viewModelScope.launch {
            if (authRepository.isLoggedIn()) {
                authRepository.getMe().fold(
                    onSuccess = { user ->
                        // In Phase 1, hasTeam is always false as team check is Phase 2
                        _state.value = AuthState.Authenticated(user, hasTeam = false)
                    },
                    onFailure = {
                        _state.value = AuthState.Unauthenticated
                    }
                )
            } else {
                _state.value = AuthState.Unauthenticated
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _state.value = AuthState.Unauthenticated
    }
}
