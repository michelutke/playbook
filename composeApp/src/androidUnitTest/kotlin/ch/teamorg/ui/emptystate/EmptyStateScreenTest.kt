package ch.teamorg.ui.emptystate

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import ch.teamorg.ui.TestActivity
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import ch.teamorg.ui.MainDispatcherRule
import ch.teamorg.ui.fakes.FakeAuthRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EmptyStateScreenTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private fun launchScreen(
        onNavigateToClubSetup: () -> Unit = {},
        onNavigateToInvite: (String) -> Unit = {},
    ): EmptyStateViewModel {
        val viewModel = EmptyStateViewModel(authRepository = FakeAuthRepository())
        composeTestRule.setContent {
            EmptyStateScreen(
                viewModel = viewModel,
                onNavigateToClubSetup = onNavigateToClubSetup,
                onNavigateToInvite = onNavigateToInvite,
            )
        }
        return viewModel
    }

    @Test
    fun emptyStateScreen_showsWelcomeText() {
        launchScreen()

        composeTestRule.onNodeWithText("Welcome to Teamorg").assertIsDisplayed()
    }

    @Test
    fun emptyStateScreen_showsInviteLinkField() {
        launchScreen()

        composeTestRule.onNodeWithTag("tf_invite_link").assertIsDisplayed()
    }

    @Test
    fun emptyStateScreen_showsJoinTeamButton() {
        launchScreen()

        composeTestRule.onNodeWithTag("btn_join_team").assertIsDisplayed()
    }

    @Test
    fun emptyStateScreen_showsSetupClubButton() {
        launchScreen()

        composeTestRule.onNodeWithTag("btn_setup_club").assertIsDisplayed()
    }

    @Test
    fun emptyStateScreen_showsCopyProfileLinkButton() {
        launchScreen()

        composeTestRule.onNodeWithTag("btn_copy_profile_link").assertIsDisplayed()
    }
}
