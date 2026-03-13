package com.playbook.ui.team

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.playbook.domain.TeamMember
import com.playbook.repository.TeamRepository
import com.playbook.ui.theme.PlaybookTheme
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class TeamRosterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val teamRepository = mockk<TeamRepository>()
    private val viewModel = TeamRosterViewModel(teamRepository)

    private val mockMembers = listOf(
        TeamMember(userId = "1", displayName = "Player One", role = "player", jerseyNumber = 10),
        TeamMember(userId = "2", displayName = "Coach One", role = "coach", position = "Head Coach")
    )

    @Test
    fun teamRosterScreen_rendersMemberList() {
        coEvery { teamRepository.getTeamRoster("team-1") } returns Result.success(mockMembers)

        composeTestRule.setContent {
            PlaybookTheme {
                TeamRosterScreen(
                    teamId = "team-1",
                    viewModel = viewModel,
                    onBack = {},
                    onShareInvite = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Team Roster").assertIsDisplayed()
        composeTestRule.onNodeWithText("Player One").assertIsDisplayed()
        composeTestRule.onNodeWithText("Coach One").assertIsDisplayed()
        composeTestRule.onNodeWithText("#10").assertIsDisplayed()
        composeTestRule.onNodeWithText("Coach • Head Coach").assertIsDisplayed()
    }

    @Test
    fun clickingFab_showsInviteDialog() {
        coEvery { teamRepository.getTeamRoster("team-1") } returns Result.success(mockMembers)

        composeTestRule.setContent {
            PlaybookTheme {
                TeamRosterScreen(
                    teamId = "team-1",
                    viewModel = viewModel,
                    onBack = {},
                    onShareInvite = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Invite Player").performClick()
        
        composeTestRule.onNodeWithText("Invite to Team").assertIsDisplayed()
        composeTestRule.onNodeWithText("Invite as Player").assertIsDisplayed()
        composeTestRule.onNodeWithText("Invite as Coach").assertIsDisplayed()
    }

    @Test
    fun longClickMember_showsConfirmationDialog() {
        coEvery { teamRepository.getTeamRoster("team-1") } returns Result.success(mockMembers)

        composeTestRule.setContent {
            PlaybookTheme {
                TeamRosterScreen(
                    teamId = "team-1",
                    viewModel = viewModel,
                    onBack = {},
                    onShareInvite = {}
                )
            }
        }

        // Long click the first member
        composeTestRule.onNodeWithText("Player One").performTouchInput { longClick() }

        composeTestRule.onNodeWithText("Remove Member").assertIsDisplayed()
        composeTestRule.onNodeWithText("Are you sure you want to remove Player One from the team?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Remove").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }
}
