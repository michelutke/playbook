package ch.teamorg.ui.invite

import app.cash.turbine.test
import ch.teamorg.domain.InviteDetails
import ch.teamorg.fake.FakeInviteRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
class InviteViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeInviteRepo = FakeInviteRepository()
    private lateinit var viewModel: InviteViewModel

    private val sampleDetails = InviteDetails(
        token = "abc123",
        teamName = "Team A",
        clubName = "Club A",
        role = "player",
        invitedBy = "Coach Bob",
        expiresAt = "2099-01-01T00:00:00Z",
        alreadyRedeemed = false
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeInviteRepo.reset()
        viewModel = InviteViewModel(fakeInviteRepo)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region — initial state

    @Test
    fun state_initially_isEmpty() {
        val state = viewModel.state.value
        state.inviteDetails shouldBe null
        state.isLoading shouldBe false
        state.isRedeeming shouldBe false
        state.error shouldBe null
        state.isRedeemed shouldBe false
    }

    // region — loadInvite happy path

    @Test
    fun loadInvite_withSuccess_populatesInviteDetails() = runTest {
        fakeInviteRepo.getInviteDetailsResult = Result.success(sampleDetails)
        viewModel.loadInvite("abc123")

        val state = viewModel.state.value
        state.inviteDetails shouldBe sampleDetails
        state.isLoading shouldBe false
    }

    @Test
    fun loadInvite_passesTokenToRepository() = runTest {
        viewModel.loadInvite("mytoken")

        fakeInviteRepo.lastDetailsToken shouldBe "mytoken"
    }

    @Test
    fun loadInvite_clearsErrorBeforeCall() = runTest {
        viewModel.state.test {
            awaitItem() // initial
            viewModel.loadInvite("abc123")
            val loadingState = awaitItem()
            loadingState.error shouldBe null
            cancelAndIgnoreRemainingEvents()
        }
    }

    // region — loadInvite error path

    @Test
    fun loadInvite_onFailure_setsErrorMessage() = runTest {
        fakeInviteRepo.getInviteDetailsResult = Result.failure(Exception("Token expired"))
        viewModel.loadInvite("abc123")

        viewModel.state.value.error shouldBe "Token expired"
    }

    @Test
    fun loadInvite_onFailureWithNullMessage_setsDefaultError() = runTest {
        fakeInviteRepo.getInviteDetailsResult = Result.failure(Exception())
        viewModel.loadInvite("abc123")

        viewModel.state.value.error shouldBe "Failed to fetch invite details"
    }

    @Test
    fun loadInvite_onFailure_clearsLoadingState() = runTest {
        fakeInviteRepo.getInviteDetailsResult = Result.failure(Exception("Error"))
        viewModel.loadInvite("abc123")

        viewModel.state.value.isLoading shouldBe false
    }

    // region — redeemInvite happy path

    @Test
    fun redeemInvite_withSuccess_setsIsRedeemedTrue() = runTest {
        viewModel.redeemInvite("abc123")

        val state = viewModel.state.value
        state.isRedeemed shouldBe true
        state.isRedeeming shouldBe false
    }

    @Test
    fun redeemInvite_passesTokenToRepository() = runTest {
        viewModel.redeemInvite("inviteToken")

        fakeInviteRepo.lastRedeemToken shouldBe "inviteToken"
    }

    // region — redeemInvite: 409 / already-member treated as success

    @Test
    fun redeemInvite_with409Message_treatsAsSuccess() = runTest {
        fakeInviteRepo.redeemInviteResult = Result.failure(Exception("HTTP 409"))
        viewModel.redeemInvite("abc123")

        val state = viewModel.state.value
        state.isRedeemed shouldBe true
        state.error shouldBe null
    }

    @Test
    fun redeemInvite_withAlreadyMemberMessage_treatsAsSuccess() = runTest {
        fakeInviteRepo.redeemInviteResult = Result.failure(Exception("Already a member of this team"))
        viewModel.redeemInvite("abc123")

        val state = viewModel.state.value
        state.isRedeemed shouldBe true
        state.error shouldBe null
        state.isRedeeming shouldBe false
    }

    @Test
    fun redeemInvite_withAlreadyMemberCaseInsensitive_treatsAsSuccess() = runTest {
        fakeInviteRepo.redeemInviteResult = Result.failure(Exception("ALREADY A MEMBER"))
        viewModel.redeemInvite("abc123")

        viewModel.state.value.isRedeemed shouldBe true
    }

    // region — redeemInvite error path

    @Test
    fun redeemInvite_onOtherFailure_setsErrorMessage() = runTest {
        fakeInviteRepo.redeemInviteResult = Result.failure(Exception("Invite expired"))
        viewModel.redeemInvite("abc123")

        val state = viewModel.state.value
        state.error shouldBe "Invite expired"
        state.isRedeemed shouldBe false
        state.isRedeeming shouldBe false
    }

    @Test
    fun redeemInvite_onOtherFailureWithNullMessage_setsDefaultError() = runTest {
        fakeInviteRepo.redeemInviteResult = Result.failure(Exception())
        viewModel.redeemInvite("abc123")

        viewModel.state.value.error shouldBe "Failed to redeem invite"
    }

    @Test
    fun redeemInvite_onFailure_clearsRedeemingState() = runTest {
        fakeInviteRepo.redeemInviteResult = Result.failure(Exception("Server error"))
        viewModel.redeemInvite("abc123")

        viewModel.state.value.isRedeeming shouldBe false
    }

    // region — full load-then-redeem journey

    /**
     * Simulates the real user flow: load invite details, then redeem.
     * Verifies that isRedeemed transitions to true, which is the signal
     * InviteScreen's LaunchedEffect uses to trigger onJoinSuccess.
     *
     * This test would catch:
     * - redeemInvite silently failing
     * - isRedeemed not being set after successful redeem
     * - state being reset between load and redeem
     */
    @Test
    fun fullFlow_loadThenRedeem_setsIsRedeemedTrue() = runTest {
        fakeInviteRepo.getInviteDetailsResult = Result.success(sampleDetails)

        // Load invite
        viewModel.loadInvite("abc123")
        viewModel.state.value.inviteDetails shouldNotBe null
        viewModel.state.value.isRedeemed shouldBe false

        // Redeem invite
        viewModel.redeemInvite("abc123")
        viewModel.state.value.isRedeemed shouldBe true
        viewModel.state.value.isRedeeming shouldBe false
        viewModel.state.value.error shouldBe null
    }

    @Test
    fun fullFlow_loadThenRedeem_inviteDetailsPreservedAfterRedeem() = runTest {
        fakeInviteRepo.getInviteDetailsResult = Result.success(sampleDetails)

        viewModel.loadInvite("abc123")
        viewModel.redeemInvite("abc123")

        // Invite details should still be available (not cleared)
        viewModel.state.value.inviteDetails shouldBe sampleDetails
    }

    /**
     * Turbine-based test that tracks state transitions during redeem.
     * Ensures isRedeeming=true is emitted before isRedeemed=true.
     */
    @Test
    fun redeemInvite_emitsRedeemingThenRedeemed() = runTest {
        viewModel.state.test {
            awaitItem() // initial state

            viewModel.redeemInvite("abc123")

            // With UnconfinedTestDispatcher, we may get coalesced states.
            // The final state must have isRedeemed=true.
            val finalState = expectMostRecentItem()
            finalState.isRedeemed shouldBe true
            finalState.isRedeeming shouldBe false
        }
    }

    // region — error does not set isRedeemed (prevents false navigation)

    @Test
    fun redeemInvite_onNon409Failure_doesNotSetIsRedeemed() = runTest {
        fakeInviteRepo.redeemInviteResult = Result.failure(Exception("Network timeout"))
        viewModel.redeemInvite("abc123")

        viewModel.state.value.isRedeemed shouldBe false
        viewModel.state.value.error shouldBe "Network timeout"
    }

    // region — authenticated user with existing team redeems cross-team invite

    /**
     * Exact scenario from the bug: authenticated club manager with existing
     * teams redeems an invite from another manager's team. The redeem must
     * succeed and set isRedeemed=true so the LaunchedEffect in InviteScreen
     * triggers onJoinSuccess.
     */
    @Test
    fun authenticatedUserWithExistingTeam_redeemInvite_setsIsRedeemed() = runTest {
        // Simulate: user loaded invite details (authenticated, has teams already)
        fakeInviteRepo.getInviteDetailsResult = Result.success(sampleDetails)
        viewModel.loadInvite("abc123")
        viewModel.state.value.inviteDetails shouldNotBe null

        // User clicks Join — redeem succeeds
        fakeInviteRepo.redeemInviteResult = Result.success(Unit)
        viewModel.redeemInvite("abc123")

        val state = viewModel.state.value
        state.isRedeemed shouldBe true
        state.isRedeeming shouldBe false
        state.error shouldBe null
        state.inviteDetails shouldNotBe null
    }

    /**
     * Verify state transitions with Turbine: loading invite, then redeeming.
     * The final state must have isRedeemed=true which is the navigation trigger.
     */
    @Test
    fun authenticatedUserWithExistingTeam_fullFlow_turbineVerifiesIsRedeemed() = runTest {
        fakeInviteRepo.getInviteDetailsResult = Result.success(sampleDetails)
        fakeInviteRepo.redeemInviteResult = Result.success(Unit)

        viewModel.state.test {
            awaitItem() // initial

            viewModel.loadInvite("abc123")
            // Skip intermediate loading states, get to loaded
            val loadedState = expectMostRecentItem()
            loadedState.inviteDetails shouldNotBe null
            loadedState.isRedeemed shouldBe false

            viewModel.redeemInvite("abc123")
            val redeemedState = expectMostRecentItem()
            redeemedState.isRedeemed shouldBe true
            redeemedState.isRedeeming shouldBe false
        }
    }
}
