package com.playbook.ui.login

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.playbook.ui.theme.PlaybookTheme
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.flow.MutableStateFlow
import com.playbook.repository.AuthRepository

class LoginScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeState = MutableStateFlow(LoginUiState())
    
    // Minimal mock-ish ViewModel
    private val fakeViewModel = object : LoginViewModel(
        authRepository = object : AuthRepository {
            override suspend fun login(request: com.playbook.domain.LoginRequest) = Result.success(com.playbook.domain.AuthResponse("", com.playbook.domain.User("", "", "")))
            override suspend fun register(request: com.playbook.domain.RegisterRequest) = Result.success(com.playbook.domain.AuthResponse("", com.playbook.domain.User("", "", "")))
            override suspend fun logout() {}
            override fun getCurrentUser() = null
        },
        onLoginSuccess = {}
    ) {
        override val state = fakeState
        override fun onEmailChange(email: String) {
            fakeState.value = fakeState.value.copy(email = email)
        }
        override fun onPasswordChange(password: String) {
            fakeState.value = fakeState.value.copy(password = password)
        }
        override fun onLoginClick() {
            if (fakeState.value.email.isEmpty() || fakeState.value.password.isEmpty()) {
                fakeState.value = fakeState.value.copy(error = "Please fill in all fields")
            } else {
                fakeState.value = fakeState.value.copy(isLoading = true)
            }
        }
    }

    @Test
    fun loginScreen_hasAllFields() {
        composeTestRule.setContent {
            PlaybookTheme {
                LoginScreen(viewModel = fakeViewModel, onNavigateToRegister = {})
            }
        }

        composeTestRule.onNodeWithText("Sign in to Playbook").assertIsDisplayed()
        composeTestRule.onNodeWithText("Email").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign in").assertIsDisplayed()
        composeTestRule.onNodeWithText("Don't have an account? Create one").assertIsDisplayed()
    }

    @Test
    fun clickingLoginWithEmptyFields_showsValidationError() {
        composeTestRule.setContent {
            PlaybookTheme {
                LoginScreen(viewModel = fakeViewModel, onNavigateToRegister = {})
            }
        }

        composeTestRule.onNodeWithText("Sign in").performClick()
        composeTestRule.onNodeWithText("Please fill in all fields").assertIsDisplayed()
    }

    @Test
    fun loginButton_disablesDuringLoadingState() {
        fakeState.value = LoginUiState(isLoading = true)
        
        composeTestRule.setContent {
            PlaybookTheme {
                LoginScreen(viewModel = fakeViewModel, onNavigateToRegister = {})
            }
        }

        composeTestRule.onNodeWithText("Sign in").assertIsNotEnabled()
    }

    @Test
    fun clickingCreateAccount_triggersNavigation() {
        var navigated = false
        composeTestRule.setContent {
            PlaybookTheme {
                LoginScreen(viewModel = fakeViewModel, onNavigateToRegister = { navigated = true })
            }
        }

        composeTestRule.onNodeWithText("Don't have an account? Create one").performClick()
        assert(navigated)
    }
}
