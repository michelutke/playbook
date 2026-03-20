package ch.teamorg.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.teamorg.domain.LoginRequest
import ch.teamorg.repository.AuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state = _state.asStateFlow()

    private val _loginSuccess = Channel<Unit>(Channel.CONFLATED)
    val loginSuccess = _loginSuccess.receiveAsFlow()

    fun onEmailChange(email: String) {
        _state.value = _state.value.copy(email = email, error = null)
    }

    fun onPasswordChange(password: String) {
        _state.value = _state.value.copy(password = password, error = null)
    }

    fun onLoginClick() {
        val currentState = _state.value
        if (currentState.isLoading) return

        if (currentState.email.isBlank() || currentState.password.isBlank()) {
            _state.value = currentState.copy(error = "Please fill in all fields")
            return
        }

        _state.value = currentState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            authRepository.login(LoginRequest(currentState.email, currentState.password)).fold(
                onSuccess = {
                    _state.value = _state.value.copy(isLoading = false)
                    _loginSuccess.send(Unit)
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Login failed"
                    )
                }
            )
        }
    }
}
