package com.playbook.auth

import com.playbook.domain.AuthUser

sealed class AuthState {
    data object Loading : AuthState()
    data object Unauthenticated : AuthState()
    data class Authenticated(val user: AuthUser, val hasTeam: Boolean) : AuthState()
}
