package com.playbook.auth

sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val clubId: String?) : AuthState
}
