package com.playbook.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playbook.preferences.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val userPreferences: UserPreferences) : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        viewModelScope.launch {
            val token = userPreferences.getToken()
            _authState.value = if (token == null) {
                AuthState.Unauthenticated
            } else {
                AuthState.Authenticated(userPreferences.getClubId())
            }
        }
    }

    fun onLogout() {
        viewModelScope.launch {
            userPreferences.clearToken()
            userPreferences.clearClubId()
            _authState.value = AuthState.Unauthenticated
        }
    }
}
