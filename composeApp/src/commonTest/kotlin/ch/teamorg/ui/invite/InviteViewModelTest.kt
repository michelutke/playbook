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
}
