package ch.teamorg.ui.register

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import ch.teamorg.ui.TestActivity
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import ch.teamorg.ui.MainDispatcherRule
import ch.teamorg.ui.fakes.FakeAuthRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RegisterScreenTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private fun launchScreen(
        onRegisterSuccess: () -> Unit = {},
        onNavigateToLogin: () -> Unit = {},
    ): RegisterViewModel {
        val viewModel = RegisterViewModel(authRepository = FakeAuthRepository())
        composeTestRule.setContent {
            RegisterScreen(
                viewModel = viewModel,
                onRegisterSuccess = onRegisterSuccess,
                onNavigateToLogin = onNavigateToLogin,
            )
        }
        return viewModel
    }

    @Test
    fun registerScreen_showsAllFieldsAndCreateAccountButton() {
        launchScreen()

        composeTestRule.onNodeWithTag("tf_display_name").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tf_email").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tf_password").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tf_confirm_password").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_create_account").assertIsDisplayed()
    }

    @Test
    fun registerScreen_shortPassword_showsValidationError() {
        launchScreen()

        composeTestRule.onNodeWithTag("tf_display_name").performTextInput("Alice")
        composeTestRule.onNodeWithTag("tf_email").performTextInput("alice@example.com")
        composeTestRule.onNodeWithTag("tf_password").performTextInput("short")
        composeTestRule.onNodeWithTag("tf_confirm_password").performTextInput("short")
        composeTestRule.onNodeWithTag("btn_create_account").performClick()

        composeTestRule.onNodeWithText("Password must be at least 8 characters").assertIsDisplayed()
    }

    @Test
    fun registerScreen_mismatchedPasswords_showsValidationError() {
        launchScreen()

        composeTestRule.onNodeWithTag("tf_display_name").performTextInput("Alice")
        composeTestRule.onNodeWithTag("tf_email").performTextInput("alice@example.com")
        composeTestRule.onNodeWithTag("tf_password").performTextInput("password123")
        composeTestRule.onNodeWithTag("tf_confirm_password").performTextInput("different456")
        composeTestRule.onNodeWithTag("btn_create_account").performClick()

        composeTestRule.onNodeWithText("Passwords do not match").assertIsDisplayed()
    }

    @Test
    fun registerScreen_tappingNavigateToLogin_callsCallback() {
        var navigated = false
        launchScreen(onNavigateToLogin = { navigated = true })

        composeTestRule.onNodeWithTag("btn_navigate_login").performClick()

        assertTrue(navigated)
    }
}
