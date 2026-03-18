package ch.teamorg.auth

import ch.teamorg.domain.AuthUser

sealed class AuthState {
    data object Loading : AuthState()
    data object Unauthenticated : AuthState()
    data class Authenticated(val user: AuthUser, val hasTeam: Boolean) : AuthState()
}
