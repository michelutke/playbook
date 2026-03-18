package ch.teamorg.domain.repositories

import ch.teamorg.domain.models.InviteDetails
import ch.teamorg.domain.models.InviteLink
import java.util.*

interface InviteRepository {
    suspend fun create(teamId: UUID, createdByUserId: UUID, role: String, email: String? = null): InviteLink
    suspend fun findByToken(token: String): InviteLink?
    suspend fun getInviteDetails(token: String): InviteDetails?
    suspend fun redeem(token: String, userId: UUID): InviteLink
    suspend fun listByTeam(teamId: UUID): List<InviteLink>
    suspend fun isMember(teamId: UUID, userId: UUID, role: String): Boolean
}
