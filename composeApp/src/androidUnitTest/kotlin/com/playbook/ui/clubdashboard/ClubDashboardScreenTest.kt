package com.playbook.ui.clubdashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.playbook.test.FakeClubRepository
import com.playbook.test.FakeMembershipRepository
import com.playbook.test.FakeTeamRepository
import com.playbook.test.TestApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestApplication::class)
class ClubDashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    private fun makeViewModel(clubId: String = "club-1") = ClubDashboardViewModel(
        clubId = clubId,
        clubRepository = FakeClubRepository(),
        teamRepository = FakeTeamRepository(),
        membershipRepository = FakeMembershipRepository(),
    )

    @Test
    fun fab_isVisible() {
        val vm = makeViewModel()
        composeTestRule.setContent {
            ClubDashboardScreen(
                clubId = "club-1",
                onNavigateToTeam = {},
                onNavigateToEdit = {},
                onNavigateToInviteCoaches = {},
                viewModel = vm,
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Create team").assertIsDisplayed()
    }

    @Test
    fun emptyState_showsTeamsSectionAndInviteCoaches() {
        val vm = makeViewModel()
        composeTestRule.setContent {
            ClubDashboardScreen(
                clubId = "club-1",
                onNavigateToTeam = {},
                onNavigateToEdit = {},
                onNavigateToInviteCoaches = {},
                viewModel = vm,
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Teams").assertIsDisplayed()
        composeTestRule.onNodeWithText("Invite Coaches").assertIsDisplayed()
    }
}
