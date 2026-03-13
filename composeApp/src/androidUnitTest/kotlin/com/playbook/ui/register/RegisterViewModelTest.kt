package com.playbook.ui.register

import com.playbook.domain.AuthResponse
import com.playbook.domain.RegisterRequest
import com.playbook.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

class RegisterViewModelTest {

    private lateinit var viewModel: RegisterViewModel
    private lateinit var repository: FakeAuthRepository
    private var navigateCalled = false

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        repository = FakeAuthRepository()
        viewModel = RegisterViewModel(repository) {
            navigateCalled = true
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state is empty`() {
        val state = viewModel.state.value
        assertEquals("", state.displayName)
        assertEquals("", state.email)
        assertFalse(state.isLoading)
    }

    @Test
    fun `test validation error when fields empty`() {
        viewModel.onRegisterClick()
        assertEquals("Please fill in all fields", viewModel.state.value.error)
    }

    @Test
    fun `test password too short`() {
        viewModel.onDisplayNameChange("Name")
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("short")
        viewModel.onConfirmPasswordChange("short")
        
        viewModel.onRegisterClick()
        
        assertEquals("Password must be at least 8 characters", viewModel.state.value.error)
    }

    @Test
    fun `test passwords do not match`() {
        viewModel.onDisplayNameChange("Name")
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("different")
        
        viewModel.onRegisterClick()
        
        assertEquals("Passwords do not match", viewModel.state.value.error)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test registration success`() = runTest {
        viewModel.onDisplayNameChange("Name")
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")
        
        viewModel.onRegisterClick()
        advanceUntilIdle()
        
        assertTrue(navigateCalled)
    }
}

class FakeAuthRepository : AuthRepository {
    override suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        return Result.success(AuthResponse("token", "id", "name", null))
    }
    override suspend fun login(request: com.playbook.domain.LoginRequest) = TODO()
    override fun logout() {}
    override fun isLoggedIn() = false
    override suspend fun getMe() = TODO()
}
