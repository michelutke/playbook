package ch.teamorg.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the backStack management logic for the invite-join flow.
 *
 * These tests verify the navigation behavior extracted from TeamorgApp and
 * AppNavigation. The core bug was: after redeeming an invite, onJoinSuccess
 * called checkAuthState() which re-triggered LaunchedEffect, but the Invite
 * screen was still in the backStack, causing the guard condition
 * `backStack.none { it is Screen.Invite }` to skip navigation entirely.
 *
 * The fix: onJoinSuccess removes all Invite screens from the backStack before
 * calling checkAuthState, so the LaunchedEffect navigates to the correct screen.
 */
class InviteJoinNavigationTest {

    // ──────────────────────────────────────────────────────────────────────────
    // Simulate the navigation logic from TeamorgApp.LaunchedEffect
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Mirrors the LaunchedEffect(authState, pendingToken) logic in TeamorgApp.
     * Returns true if navigation was performed.
     */
    private fun simulateAuthStateChange(
        backStack: MutableList<Screen>,
        isAuthenticated: Boolean,
        hasTeam: Boolean,
        pendingToken: String?
    ): Boolean {
        if (!isAuthenticated) {
            backStack.clear()
            backStack.add(Screen.Login)
            return true
        }

        // Authenticated
        if (pendingToken != null) {
            backStack.clear()
            backStack.add(if (!hasTeam) Screen.EmptyState else Screen.Events)
            backStack.add(Screen.Invite(pendingToken))
            return true
        } else if (backStack.none { it is Screen.Invite }) {
            backStack.clear()
            backStack.add(if (!hasTeam) Screen.EmptyState else Screen.Events)
            return true
        }

        return false // No navigation performed
    }

    /**
     * Mirrors the onJoinSuccess callback in AppNavigation.
     * This is the FIX: removes Invite screens before triggering checkAuthState.
     */
    private fun simulateJoinSuccess(backStack: MutableList<Screen>) {
        backStack.removeAll { it is Screen.Invite }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Bug reproduction: without the fix, join does nothing
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun bugRepro_withoutInviteRemoval_authStateChangeSkipsNavigation() {
        // Setup: user is on invite screen (authenticated, viewing invite)
        val backStack = mutableListOf<Screen>(Screen.Events, Screen.Invite("token123"))

        // Simulate what happens WITHOUT the fix: checkAuthState fires but
        // Invite is still in backStack
        val navigated = simulateAuthStateChange(
            backStack = backStack,
            isAuthenticated = true,
            hasTeam = true,
            pendingToken = null
        )

        // BUG: navigation is skipped because Invite is in backStack
        assertFalse(navigated, "Without fix, navigation is skipped when Invite is in backStack")
        assertTrue(backStack.any { it is Screen.Invite }, "Invite screen remains stuck in backStack")
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Fix verification: with Invite removal, navigation proceeds
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun fix_joinSuccessRemovesInvite_thenAuthStateNavigatesToEvents() {
        val backStack = mutableListOf<Screen>(Screen.Events, Screen.Invite("token123"))

        // Step 1: onJoinSuccess removes Invite from backStack (THE FIX)
        simulateJoinSuccess(backStack)
        assertFalse(backStack.any { it is Screen.Invite }, "Invite must be removed after join")

        // Step 2: checkAuthState triggers LaunchedEffect -> simulateAuthStateChange
        val navigated = simulateAuthStateChange(
            backStack = backStack,
            isAuthenticated = true,
            hasTeam = true, // user now has a team after redeeming
            pendingToken = null
        )

        assertTrue(navigated, "Navigation must proceed after Invite is removed")
        assertEquals(1, backStack.size)
        assertEquals(Screen.Events, backStack.first(), "Should navigate to Events when hasTeam=true")
    }

    @Test
    fun fix_joinSuccessRemovesInvite_navigatesToEmptyStateWhenNoTeam() {
        // Edge case: hasTeam is still false (maybe server hasn't updated yet)
        val backStack = mutableListOf<Screen>(Screen.EmptyState, Screen.Invite("token123"))

        simulateJoinSuccess(backStack)
        val navigated = simulateAuthStateChange(
            backStack = backStack,
            isAuthenticated = true,
            hasTeam = false,
            pendingToken = null
        )

        assertTrue(navigated)
        assertEquals(1, backStack.size)
        assertEquals(Screen.EmptyState, backStack.first())
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Deep link + invite flow
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun deepLink_authenticatedWithPendingToken_navigatesToInviteScreen() {
        val backStack = mutableListOf<Screen>(Screen.Loading)

        val navigated = simulateAuthStateChange(
            backStack = backStack,
            isAuthenticated = true,
            hasTeam = false,
            pendingToken = "deep-link-token"
        )

        assertTrue(navigated)
        assertEquals(2, backStack.size)
        assertEquals(Screen.EmptyState, backStack[0])
        assertTrue(backStack[1] is Screen.Invite)
        assertEquals("deep-link-token", (backStack[1] as Screen.Invite).token)
    }

    @Test
    fun deepLink_authenticatedWithTeamAndPendingToken_navigatesToInviteOverEvents() {
        val backStack = mutableListOf<Screen>(Screen.Loading)

        val navigated = simulateAuthStateChange(
            backStack = backStack,
            isAuthenticated = true,
            hasTeam = true,
            pendingToken = "another-token"
        )

        assertTrue(navigated)
        assertEquals(2, backStack.size)
        assertEquals(Screen.Events, backStack[0])
        assertTrue(backStack[1] is Screen.Invite)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Unauthenticated flow
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun unauthenticated_clearsBackStackAndShowsLogin() {
        val backStack = mutableListOf<Screen>(Screen.Events, Screen.Invite("token"))

        val navigated = simulateAuthStateChange(
            backStack = backStack,
            isAuthenticated = false,
            hasTeam = false,
            pendingToken = null
        )

        assertTrue(navigated)
        assertEquals(1, backStack.size)
        assertEquals(Screen.Login, backStack.first())
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Full journey simulation
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun fullJourney_deepLinkWhileUnauthenticated_loginThenRedeem_navigatesToEvents() {
        val backStack = mutableListOf<Screen>(Screen.Loading)
        var pendingToken: String? = "invite-token-abc"

        // 1. Deep link arrives, user not authenticated -> Login screen
        simulateAuthStateChange(backStack, isAuthenticated = false, hasTeam = false, pendingToken = pendingToken)
        assertEquals(Screen.Login, backStack.last())

        // 2. User logs in -> Authenticated with pending token -> Invite screen
        simulateAuthStateChange(backStack, isAuthenticated = true, hasTeam = false, pendingToken = pendingToken)
        pendingToken = null // cleared after being consumed
        assertTrue(backStack.last() is Screen.Invite)

        // 3. User clicks Join -> onJoinSuccess -> remove Invite + checkAuthState
        simulateJoinSuccess(backStack)
        assertFalse(backStack.any { it is Screen.Invite })

        // 4. checkAuthState returns Authenticated with hasTeam=true
        val navigated = simulateAuthStateChange(backStack, isAuthenticated = true, hasTeam = true, pendingToken = pendingToken)
        assertTrue(navigated)
        assertEquals(Screen.Events, backStack.last())
        assertEquals(1, backStack.size, "BackStack should only contain Events")
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Bug scenario: authenticated user with existing team redeems invite
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Exact reproduction of the reported bug: a club manager who already has
     * their own team opens an invite link from another manager. They see the
     * invite screen, click Join, and nothing happens.
     *
     * Root cause: onJoinSuccess called onAuthSuccess() without removing
     * Invite from backStack. The guard `backStack.none { it is Screen.Invite }`
     * prevented navigation.
     */
    @Test
    fun authenticatedUserWithExistingTeam_redeemInvite_navigatesToEvents() {
        val backStack = mutableListOf<Screen>(Screen.Events)
        var pendingToken: String? = "cross-team-token"

        // 1. User is on Events (already authenticated, has teams)
        // Deep link arrives with invite from another team
        simulateAuthStateChange(backStack, isAuthenticated = true, hasTeam = true, pendingToken = pendingToken)
        pendingToken = null

        // Verify: backStack = [Events, Invite(token)]
        assertEquals(2, backStack.size)
        assertEquals(Screen.Events, backStack[0])
        assertTrue(backStack[1] is Screen.Invite)

        // 2. User clicks Join -> onJoinSuccess
        simulateJoinSuccess(backStack)

        // Verify: Invite removed, only Events remains
        assertFalse(backStack.any { it is Screen.Invite })
        assertEquals(1, backStack.size)
        assertEquals(Screen.Events, backStack[0])

        // 3. checkAuthState fires -> Authenticated with hasTeam=true
        val navigated = simulateAuthStateChange(
            backStack, isAuthenticated = true, hasTeam = true, pendingToken = null
        )
        assertTrue(navigated, "Navigation must proceed to Events for user with existing teams")
        assertEquals(1, backStack.size)
        assertEquals(Screen.Events, backStack.first())
    }

    /**
     * Verify that without the fix (no removeAll), the authenticated user
     * with existing teams gets stuck on the invite screen.
     */
    @Test
    fun bugRepro_authenticatedUserWithExistingTeam_withoutFix_getsStuck() {
        val backStack = mutableListOf<Screen>(Screen.Events)
        var pendingToken: String? = "cross-team-token"

        // Deep link opens invite
        simulateAuthStateChange(backStack, isAuthenticated = true, hasTeam = true, pendingToken = pendingToken)
        pendingToken = null
        assertTrue(backStack.last() is Screen.Invite)

        // BUG: onJoinSuccess only calls checkAuthState WITHOUT removing Invite
        // (simulated by skipping simulateJoinSuccess)
        val navigated = simulateAuthStateChange(
            backStack, isAuthenticated = true, hasTeam = true, pendingToken = null
        )

        // Navigation is skipped because Invite is still in backStack
        assertFalse(navigated, "Without fix: guard prevents navigation when Invite is in backStack")
        assertTrue(backStack.any { it is Screen.Invite }, "Without fix: user stuck on Invite screen")
    }
}
