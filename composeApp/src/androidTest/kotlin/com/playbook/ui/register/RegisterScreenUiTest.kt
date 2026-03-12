package com.playbook.ui.register

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.playbook.ui.theme.PlaybookTheme
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.flow.MutableStateFlow
import com.playbook.repository.AuthRepository

class RegisterScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeState = MutableStateFlow(RegisterUiState())
    
    private val fakeViewModel = object : RegisterViewModel(
        authRepository = object : AuthRepository {
            override suspend fun login(request: com.playbook.domain.LoginRequest) = Result.success(com.playbook.domain.AuthResponse("", com.playbook.domain.User("", "", "")))
            override suspend fun register(request: com.playbook.domain.RegisterRequest) = Result.success(com.playbook.domain.AuthResponse("", com.playbook.domain.User("", "", "")))
            override suspend fun logout() {}
            override fun getCurrentUser() = null
        },
        onRegisterSuccess = {}
    ) {
        override val state = fakeState
        override fun onDisplayNameChange(name: String) { fakeState.value = fakeState.value.copy(displayName = name) }
        override fun onEmailChange(email: String) { fakeState.value = fakeState.value.copy(email = email) }
        override fun onPasswordChange(password: String) { fakeState.value = fakeState.value.copy(password = password) }
        override fun onConfirmPasswordChange(password: String) { fakeState.value = fakeState.value.copy(confirmPassword = password) }
        
        override fun onRegisterClick() {
            val s = fakeState.value
            if (s.password != s.confirmPassword) {
                fakeState.value = s.copy(error = "Passwords do not match")
            } else if (!s.email.contains("@")) {
                fakeState.value = s.copy(error = "Invalid email format")
            } else {
                fakeState.value = s.copy(isLoading = true)
            }
        }
    }

    @Test
    fun passwordMismatch_showsError() {
        composeTestRule.setContent {
            PlaybookTheme {
                RegisterScreen(viewModel = fakeViewModel, onNavigateToLogin = {})
            }
        }

        fakeState.value = RegisterUiState(
            displayName = "Test",
            email = "test@example.com",
            password = "password123",
            confirmPassword = "different"
        )

        composeTestRule.onNodeWithText("Create Account").performClick()
        composeTestRule.onNodeWithText("Passwords do not match").assertIsDisplayed()
    }

    @Test
    fun invalidEmailFormat_showsError() {
        composeTestRule.setContent {
            PlaybookTheme {
                RegisterScreen(viewModel = fakeViewModel, onNavigateToLogin = {})
            }
        }

        fakeState.value = RegisterUiState(
            displayName = "Test",
            email = "invalid-email",
            password = "password123",
            confirmPassword = "password123"
        )

        composeTestRule.onNodeWithText("Create Account").performClick()
        composeTestRule.onNodeWithText("Invalid email format").assertIsDisplayed()
    }

    @Test
    fun registerButton_disablesDuringLoading() {
        fakeState.value = RegisterUiState(isLoading = true)

        composeTestRule.setContent {
            PlaybookTheme {
                RegisterScreen(viewModel = fakeViewModel, onNavigateToLogin = {})
            }
        }

        composeTestRule.onNodeWithText("Create Account").assertIsNotEnabled()
    }
}
