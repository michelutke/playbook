package com.playbook.ui.teamdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.playbook.domain.MemberRole
import com.playbook.domain.RosterMember
import com.playbook.domain.Team
import com.playbook.domain.TeamStatus
import com.playbook.test.FakeMembershipRepository
import com.playbook.test.FakeTeamRepository
import com.playbook.test.TestApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestApplication::class)
class TeamDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    private fun makeTeam() = Team(
        id = "team-1",
        clubId = "club-1",
        name = "Test Team",
        description = null,
        status = TeamStatus.ACTIVE,
        requestedBy = null,
        rejectionReason = null,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
    )

    private fun makeCoach() = RosterMember(
        userId = "coach-1",
        displayName = "Coach Alice",
        avatarUrl = null,
        roles = listOf(MemberRole.COACH),
        joinedAt = Clock.System.now(),
    )

    @Test
    fun rosterTab_showsCoachesSectionHeader() {
        val vm = TeamDetailViewModel(
            teamId = "team-1",
            clubId = "club-1",
            membershipRepository = FakeMembershipRepository(roster = listOf(makeCoach())),
            teamRepository = FakeTeamRepository(team = makeTeam()),
        )
        composeTestRule.setContent {
            TeamDetailScreen(
                teamId = "team-1",
                clubId = "club-1",
                onNavigateBack = {},
                viewModel = vm,
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Coaches").assertIsDisplayed()
    }

    @Test
    fun settingsTab_showsLeaveTeamButton() {
        val vm = TeamDetailViewModel(
            teamId = "team-1",
            clubId = "club-1",
            membershipRepository = FakeMembershipRepository(),
            teamRepository = FakeTeamRepository(team = makeTeam()),
        )
        composeTestRule.setContent {
            TeamDetailScreen(
                teamId = "team-1",
                clubId = "club-1",
                onNavigateBack = {},
                viewModel = vm,
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        // Leave Team is inside a LazyColumn — scroll it into view then assert
        composeTestRule.onNodeWithText("Leave Team").performScrollTo()
        composeTestRule.onNodeWithText("Leave Team").assertIsDisplayed()
    }
}
