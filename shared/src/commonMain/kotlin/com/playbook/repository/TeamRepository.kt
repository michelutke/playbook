package com.playbook.repository

import com.playbook.domain.TeamMember

interface TeamRepository {
    suspend fun getTeamRoster(teamId: String): Result<List<TeamMember>>
    suspend fun removeMember(teamId: String, userId: String): Result<Unit>
    suspend fun createInvite(teamId: String, role: String, email: String?): Result<String>
}
