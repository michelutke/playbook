package com.playbook.ui.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.playbook.auth.AuthViewModel
import com.playbook.preferences.UserPreferences
import com.playbook.repository.AuthRepository
import com.playbook.test.FailingAuthRepository
import com.playbook.test.FakeAuthRepository
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
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    private fun makeViewModel(authRepo: AuthRepository = FakeAuthRepository()): LoginViewModel {
        val prefs = UserPreferences(ApplicationProvider.getApplicationContext())
        return LoginViewModel(
            authRepository = authRepo,
            userPreferences = prefs,
            authViewModel = AuthViewModel(prefs),
        )
    }

    @Test
    fun emailField_isDisplayed() {
        val vm = makeViewModel()
        composeTestRule.setContent { LoginScreen(viewModel = vm) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Email").assertIsDisplayed()
    }

    @Test
    fun emptySubmit_showsError() {
        val vm = makeViewModel(authRepo = FailingAuthRepository())
        composeTestRule.setContent { LoginScreen(viewModel = vm) }
        composeTestRule.waitForIdle()
        // Click the Login button — there are two nodes with "Login" text (title + button),
        // pick the last one which is the button
        composeTestRule.onAllNodesWithText("Login")[1].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Authentication failed.").assertIsDisplayed()
    }

    @Test
    fun toggleToRegisterMode_showsDisplayNameField() {
        val vm = makeViewModel()
        composeTestRule.setContent { LoginScreen(viewModel = vm) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Switch to Register").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Display name (optional)").assertIsDisplayed()
    }
}
