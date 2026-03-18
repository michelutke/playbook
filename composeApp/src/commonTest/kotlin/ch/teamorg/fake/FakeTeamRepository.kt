package ch.teamorg.fake

import ch.teamorg.domain.TeamMember
import ch.teamorg.repository.TeamRepository

class FakeTeamRepository : TeamRepository {

    var getRosterResult: Result<List<TeamMember>> = Result.success(emptyList())
    var removeMemberResult: Result<Unit> = Result.success(Unit)
    var createInviteResult: Result<String> = Result.success("https://teamorg.app/invite/abc123")

    var lastRosterTeamId: String? = null
    var lastRemovedTeamId: String? = null
    var lastRemovedUserId: String? = null
    var lastInviteTeamId: String? = null
    var lastInviteRole: String? = null

    fun reset() {
        getRosterResult = Result.success(emptyList())
        removeMemberResult = Result.success(Unit)
        createInviteResult = Result.success("https://teamorg.app/invite/abc123")
        lastRosterTeamId = null
        lastRemovedTeamId = null
        lastRemovedUserId = null
        lastInviteTeamId = null
        lastInviteRole = null
    }

    override suspend fun getTeamRoster(teamId: String): Result<List<TeamMember>> {
        lastRosterTeamId = teamId
        return getRosterResult
    }

    override suspend fun removeMember(teamId: String, userId: String): Result<Unit> {
        lastRemovedTeamId = teamId
        lastRemovedUserId = userId
        return removeMemberResult
    }

    override suspend fun createInvite(teamId: String, role: String, email: String?): Result<String> {
        lastInviteTeamId = teamId
        lastInviteRole = role
        return createInviteResult
    }
}
