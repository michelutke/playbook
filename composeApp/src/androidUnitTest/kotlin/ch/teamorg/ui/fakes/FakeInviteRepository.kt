package ch.teamorg.ui.fakes

import ch.teamorg.domain.InviteDetails
import ch.teamorg.repository.InviteRepository

class FakeInviteRepository : InviteRepository {
    var getInviteDetailsResult: Result<InviteDetails> = Result.failure(RuntimeException("Not configured"))
    var redeemInviteResult: Result<Unit> = Result.success(Unit)

    override suspend fun getInviteDetails(token: String): Result<InviteDetails> = getInviteDetailsResult
    override suspend fun redeemInvite(token: String): Result<Unit> = redeemInviteResult
}
