package com.playbook.ui.invite

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.playbook.domain.InviteDetails
import com.playbook.repository.InviteRepository
import com.playbook.ui.theme.PlaybookTheme
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class InviteScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val inviteRepository = mockk<InviteRepository>()
    private val viewModel = InviteViewModel(inviteRepository)

    private val mockInvite = InviteDetails(
        teamId = "team-1",
        teamName = "Titans",
        clubName = "Olympus Club",
        invitedBy = "Coach Zeus",
        role = "player"
    )

    @Test
    fun inviteScreen_rendersInviteDetails() {
        coEvery { inviteRepository.getInviteDetails("token-123") } returns Result.success(mockInvite)

        composeTestRule.setContent {
            PlaybookTheme {
                InviteScreen(
                    token = "token-123",
                    viewModel = viewModel,
                    isLoggedIn = true,
                    onNavigateToLogin = {},
                    onNavigateToRegister = {},
                    onJoinSuccess = {}
                )
            }
        }

        composeTestRule.onNodeWithText("You've been invited to join").assertIsDisplayed()
        composeTestRule.onNodeWithText("Titans").assertIsDisplayed()
        composeTestRule.onNodeWithText("at Olympus Club").assertIsDisplayed()
        composeTestRule.onNodeWithText("Invited by Coach Zeus as Player").assertIsDisplayed()
        composeTestRule.onNodeWithText("Join Titans").assertIsDisplayed()
    }

    @Test
    fun joinButton_callsRedeemAndNavigates() {
        coEvery { inviteRepository.getInviteDetails("token-123") } returns Result.success(mockInvite)
        coEvery { inviteRepository.redeemInvite("token-123") } returns Result.success(Unit)

        var joined = false
        composeTestRule.setContent {
            PlaybookTheme {
                InviteScreen(
                    token = "token-123",
                    viewModel = viewModel,
                    isLoggedIn = true,
                    onNavigateToLogin = {},
                    onNavigateToRegister = {},
                    onJoinSuccess = { joined = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Join Titans").performClick()

        composeTestRule.waitUntil(timeoutMillis = 3000) { joined }
        assert(joined)
    }

    @Test
    fun expiredInvite_showsErrorState() {
        coEvery { inviteRepository.getInviteDetails("expired-token") } returns Result.failure(Exception("This invite has expired"))

        composeTestRule.setContent {
            PlaybookTheme {
                InviteScreen(
                    token = "expired-token",
                    viewModel = viewModel,
                    isLoggedIn = true,
                    onNavigateToLogin = {},
                    onNavigateToRegister = {},
                    onJoinSuccess = {}
                )
            }
        }

        composeTestRule.onNodeWithText("This invite has expired").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun unauthenticatedUser_showsCreateAccountButton() {
        coEvery { inviteRepository.getInviteDetails("token-123") } returns Result.success(mockInvite)

        composeTestRule.setContent {
            PlaybookTheme {
                InviteScreen(
                    token = "token-123",
                    viewModel = viewModel,
                    isLoggedIn = false,
                    onNavigateToLogin = {},
                    onNavigateToRegister = {},
                    onJoinSuccess = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Create Account to Join").assertIsDisplayed()
        composeTestRule.onNodeWithText("Login to Join").assertIsDisplayed()
        composeTestRule.onNodeWithText("Join Titans").assertDoesNotExist()
    }
}
