package ch.teamorg.fake

import ch.teamorg.domain.InviteDetails
import ch.teamorg.repository.InviteRepository

class FakeInviteRepository : InviteRepository {

    var getInviteDetailsResult: Result<InviteDetails> = Result.success(
        InviteDetails(
            token = "abc123",
            teamName = "Team A",
            clubName = "Club A",
            role = "player",
            invitedBy = "Coach",
            expiresAt = "2099-01-01T00:00:00Z",
            alreadyRedeemed = false
        )
    )
    var redeemInviteResult: Result<Unit> = Result.success(Unit)

    var lastDetailsToken: String? = null
    var lastRedeemToken: String? = null

    fun reset() {
        getInviteDetailsResult = Result.success(
            InviteDetails(
                token = "abc123",
                teamName = "Team A",
                clubName = "Club A",
                role = "player",
                invitedBy = "Coach",
                expiresAt = "2099-01-01T00:00:00Z",
                alreadyRedeemed = false
            )
        )
        redeemInviteResult = Result.success(Unit)
        lastDetailsToken = null
        lastRedeemToken = null
    }

    override suspend fun getInviteDetails(token: String): Result<InviteDetails> {
        lastDetailsToken = token
        return getInviteDetailsResult
    }

    override suspend fun redeemInvite(token: String): Result<Unit> {
        lastRedeemToken = token
        return redeemInviteResult
    }
}
