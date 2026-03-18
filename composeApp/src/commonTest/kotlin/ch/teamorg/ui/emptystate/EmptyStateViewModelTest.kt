package ch.teamorg.ui.emptystate

import app.cash.turbine.test
import ch.teamorg.domain.AuthUser
import ch.teamorg.fake.FakeAuthRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EmptyStateViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeAuth = FakeAuthRepository()
    private lateinit var viewModel: EmptyStateViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuth.reset()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = EmptyStateViewModel(fakeAuth).also { viewModel = it }

    // region — init: profile link loading

    @Test
    fun init_withGetMeSuccess_setsProfileLink() = runTest {
        fakeAuth.getMeResult = Result.success(
            AuthUser(userId = "user42", email = "a@b.com", displayName = "Alice", avatarUrl = null)
        )
        createViewModel()

        viewModel.state.value.profileLink shouldBe "teamorg://invite/player/user42"
    }

    @Test
    fun init_profileLink_containsUserId() = runTest {
        fakeAuth.getMeResult = Result.success(
            AuthUser(userId = "abc-xyz", email = "a@b.com", displayName = "Alice", avatarUrl = null)
        )
        createViewModel()

        viewModel.state.value.profileLink shouldContain "abc-xyz"
    }

    @Test
    fun init_withGetMeFailure_profileLinkRemainsEmpty() = runTest {
        fakeAuth.getMeResult = Result.failure(Exception("Not authenticated"))
        createViewModel()

        viewModel.state.value.profileLink shouldBe ""
    }

    // region — field updates

    @Test
    fun onInviteLinkChange_updatesInviteLinkAndClearsError() = runTest {
        createViewModel()
        viewModel.state.test {
            awaitItem() // initial (may include profile link emission)

            viewModel.onInviteLinkChange("teamorg://invite/team/abc")
            val state = awaitItem()
            state.inviteLink shouldBe "teamorg://invite/team/abc"
            state.error shouldBe null

            cancelAndIgnoreRemainingEvents()
        }
    }

    // region — onJoinTeamClick (Phase 2 stub)

    @Test
    fun onJoinTeamClick_setsInfoMessageAboutPhase2() = runTest {
        createViewModel()
        viewModel.onJoinTeamClick()

        viewModel.state.value.infoMessage shouldBe "Team joining will be available in Phase 2"
    }

    @Test
    fun onJoinTeamClick_doesNotEmitNavigateEvent() = runTest {
        createViewModel()
        viewModel.events.test {
            viewModel.onJoinTeamClick()
            // No event should be emitted (early return in Phase 2 stub)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // region — onCreateClubClick

    @Test
    fun onCreateClubClick_emitsNavigateToClubSetupEvent() = runTest {
        createViewModel()
        viewModel.events.test {
            viewModel.onCreateClubClick()
            awaitItem() shouldBe EmptyStateEvent.NavigateToClubSetup
            cancelAndIgnoreRemainingEvents()
        }
    }

    // region — onProfileLinkCopied

    @Test
    fun onProfileLinkCopied_setsInfoMessage() = runTest {
        createViewModel()
        viewModel.onProfileLinkCopied()

        viewModel.state.value.infoMessage shouldBe "Link copied to clipboard"
    }

    // region — dismissMessages

    @Test
    fun dismissMessages_clearsErrorAndInfoMessage() = runTest {
        createViewModel()
        viewModel.onProfileLinkCopied() // sets infoMessage
        viewModel.dismissMessages()

        val state = viewModel.state.value
        state.error shouldBe null
        state.infoMessage shouldBe null
    }

    @Test
    fun dismissMessages_withOnlyError_clearsError() = runTest {
        createViewModel()
        // manually trigger an error-like scenario via field update + check dismissal
        viewModel.onInviteLinkChange("link")
        viewModel.dismissMessages()

        viewModel.state.value.error shouldBe null
    }
}
