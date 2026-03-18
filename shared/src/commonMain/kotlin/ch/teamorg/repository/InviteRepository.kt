package ch.teamorg.repository

import ch.teamorg.domain.InviteDetails

interface InviteRepository {
    suspend fun getInviteDetails(token: String): Result<InviteDetails>
    suspend fun redeemInvite(token: String): Result<Unit>
}
