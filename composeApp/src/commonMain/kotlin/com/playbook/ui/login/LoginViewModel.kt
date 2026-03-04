package com.playbook.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playbook.auth.AuthViewModel
import com.playbook.domain.LoginRequest
import com.playbook.domain.RegisterRequest
import com.playbook.preferences.UserPreferences
import com.playbook.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val isRegisterMode: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences,
    private val authViewModel: AuthViewModel,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun toggleMode() {
        _uiState.update { it.copy(isRegisterMode = !it.isRegisterMode, error = null) }
    }

    fun onEmailChange(v: String) { _uiState.update { it.copy(email = v) } }
    fun onPasswordChange(v: String) { _uiState.update { it.copy(password = v) } }
    fun onDisplayNameChange(v: String) { _uiState.update { it.copy(displayName = v) } }

    fun submit() {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val token = if (state.isRegisterMode) {
                    authRepository.register(
                        RegisterRequest(
                            email = state.email,
                            password = state.password,
                            displayName = state.displayName.takeIf { it.isNotBlank() },
                        )
                    ).token
                } else {
                    authRepository.login(
                        LoginRequest(email = state.email, password = state.password)
                    ).token
                }
                userPreferences.saveToken(token)
                val profile = authRepository.getMe(token)
                val clubId = profile.clubId
                if (clubId != null) userPreferences.saveClubId(clubId)
                _uiState.update { it.copy(isLoading = false) }
                authViewModel.onLoginSuccess(clubId)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Authentication failed.") }
            }
        }
    }
}
