package com.playbook.auth

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AuthStateTest {

    @Test
    fun authState_unauthenticated_isDistinctFromAuthenticated() {
        val unauthenticated: AuthState = AuthState.Unauthenticated
        val authenticated: AuthState = AuthState.Authenticated(clubId = "club-1")

        assertNotEquals<AuthState>(unauthenticated, authenticated)
        assertFalse(unauthenticated is AuthState.Authenticated)
        assertTrue(authenticated is AuthState.Authenticated)
    }

    @Test
    fun authState_authenticated_holdsClubId() {
        val state = AuthState.Authenticated(clubId = "club-42")

        assertTrue(state is AuthState.Authenticated)
        assertTrue(state.clubId == "club-42")
    }

    @Test
    fun authState_authenticated_withNullClubId_isValid() {
        val state = AuthState.Authenticated(clubId = null)

        assertTrue(state is AuthState.Authenticated)
        assertTrue(state.clubId == null)
    }

    @Test
    fun authState_loading_isDistinctFromOtherStates() {
        val loading: AuthState = AuthState.Loading
        val unauthenticated: AuthState = AuthState.Unauthenticated

        assertNotEquals<AuthState>(loading, unauthenticated)
        assertFalse(loading is AuthState.Authenticated)
    }
}
