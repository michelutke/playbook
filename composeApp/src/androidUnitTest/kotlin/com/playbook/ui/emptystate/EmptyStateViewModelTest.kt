package com.playbook.ui.emptystate

import com.playbook.domain.AuthUser
import com.playbook.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

class EmptyStateViewModelTest {

    private lateinit var viewModel: EmptyStateViewModel
    private lateinit var repository: FakeAuthRepository

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeAuthRepository()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test profile link is loaded on init`() = runTest {
        viewModel = EmptyStateViewModel(repository)
        
        // Advance time for launched effect
        testScheduler.advanceUntilIdle()
        
        assertEquals("playbook://invite/player/user-123", viewModel.state.value.profileLink)
    }

    @Test
    fun `test copy profile link updates message`() {
        viewModel = EmptyStateViewModel(repository)
        viewModel.onProfileLinkCopied()
        assertEquals("Link copied to clipboard", viewModel.state.value.infoMessage)
    }

    @Test
    fun `test join team message`() {
        viewModel = EmptyStateViewModel(repository)
        viewModel.onJoinTeamClick()
        assertEquals("Team joining will be available in Phase 2", viewModel.state.value.infoMessage)
    }
}

class FakeAuthRepository : AuthRepository {
    override suspend fun register(request: com.playbook.domain.RegisterRequest) = TODO()
    override suspend fun login(request: com.playbook.domain.LoginRequest) = TODO()
    override fun logout() {}
    override fun isLoggedIn() = true
    override suspend fun getMe(): Result<AuthUser> {
        return Result.success(AuthUser("user-123", "test@test.com", "Test User", null))
    }
}
