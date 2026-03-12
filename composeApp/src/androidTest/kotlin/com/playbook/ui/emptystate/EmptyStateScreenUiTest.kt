package com.playbook.ui.emptystate

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.playbook.ui.theme.PlaybookTheme
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.flow.MutableStateFlow
import com.playbook.repository.AuthRepository

class EmptyStateScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeState = MutableStateFlow(EmptyStateUiState(profileLink = "playbook://invite/player/123"))
    
    private val fakeViewModel = object : EmptyStateViewModel(
        authRepository = object : AuthRepository {
            override suspend fun login(request: com.playbook.domain.LoginRequest) = Result.success(com.playbook.domain.AuthResponse("", com.playbook.domain.User("", "", "")))
            override suspend fun register(request: com.playbook.domain.RegisterRequest) = Result.success(com.playbook.domain.AuthResponse("", com.playbook.domain.User("", "", "")))
            override suspend fun logout() {}
            override fun getCurrentUser() = null
            override suspend fun getMe() = Result.success(com.playbook.domain.User("123", "Test", "test@test.com"))
        }
    ) {
        override val state = fakeState
        override fun onInviteLinkChange(link: String) { fakeState.value = fakeState.value.copy(inviteLink = link) }
        override fun onJoinTeamClick() { fakeState.value = fakeState.value.copy(infoMessage = "Joining...") }
        override fun onCreateClubClick() { fakeState.value = fakeState.value.copy(infoMessage = "Creating...") }
        override fun onProfileLinkCopied() { fakeState.value = fakeState.value.copy(infoMessage = "Link copied to clipboard") }
        override fun dismissMessages() { fakeState.value = fakeState.value.copy(infoMessage = null, error = null) }
    }

    @Test
    fun sectionsRenderCorrectly() {
        composeTestRule.setContent {
            PlaybookTheme {
                EmptyStateScreen(viewModel = fakeViewModel)
            }
        }

        composeTestRule.onNodeWithText("Welcome to Playbook").assertIsDisplayed()
        composeTestRule.onNodeWithText("Join a team").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create a club").assertIsDisplayed()
        composeTestRule.onNodeWithText("Let a coach add you").assertIsDisplayed()
    }

    @Test
    fun clickingCopyLink_showsConfirmation() {
        composeTestRule.setContent {
            PlaybookTheme {
                EmptyStateScreen(viewModel = fakeViewModel)
            }
        }

        composeTestRule.onNodeWithContentDescription("Copy").performClick()
        composeTestRule.onNodeWithText("Link copied to clipboard").assertIsDisplayed()
    }

    @Test
    fun joinTeamClick_showsInfoMessage() {
        composeTestRule.setContent {
            PlaybookTheme {
                EmptyStateScreen(viewModel = fakeViewModel)
            }
        }

        composeTestRule.onNodeWithText("Join Team").performClick()
        composeTestRule.onNodeWithText("Joining...").assertIsDisplayed()
    }
}
