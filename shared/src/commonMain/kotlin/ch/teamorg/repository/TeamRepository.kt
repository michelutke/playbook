package ch.teamorg.repository

import ch.teamorg.domain.TeamMember
import ch.teamorg.domain.UserRoles

interface TeamRepository {
    suspend fun getTeamRoster(teamId: String): Result<List<TeamMember>>
    suspend fun removeMember(teamId: String, userId: String): Result<Unit>
    suspend fun createInvite(teamId: String, role: String, email: String?): Result<String>
    suspend fun getMyRoles(): Result<UserRoles>
    suspend fun updateMemberRole(teamId: String, userId: String, role: String): Result<TeamMember>
}
