package com.playbook.ui.login

import com.playbook.domain.AuthResponse
import com.playbook.domain.LoginRequest
import com.playbook.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

class LoginViewModelTest {

    private lateinit var viewModel: LoginViewModel
    private lateinit var repository: FakeAuthRepository
    private var navigateCalled = false

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        repository = FakeAuthRepository()
        viewModel = LoginViewModel(repository) {
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
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `test validation error when fields empty`() {
        viewModel.onLoginClick()
        assertEquals("Please fill in all fields", viewModel.state.value.error)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test login success`() = runTest {
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password")
        
        viewModel.onLoginClick()
        advanceUntilIdle()
        
        assertTrue(navigateCalled)
        assertFalse(viewModel.state.value.isLoading)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test login failure shows error`() = runTest {
        repository.shouldFail = true
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password")
        
        viewModel.onLoginClick()
        advanceUntilIdle()
        
        assertFalse(navigateCalled)
        assertEquals("Login failed", viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
    }
}

class FakeAuthRepository : AuthRepository {
    var shouldFail = false

    override suspend fun register(request: com.playbook.domain.RegisterRequest) = TODO()
    
    override suspend fun login(request: LoginRequest): Result<AuthResponse> {
        return if (shouldFail) {
            Result.failure(Exception("Login failed"))
        } else {
            Result.success(AuthResponse("token", "id", "name", null))
        }
    }

    override fun logout() {}
    override fun isLoggedIn() = false
    override suspend fun getMe() = TODO()
}
