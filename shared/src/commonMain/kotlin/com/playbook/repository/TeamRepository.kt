package com.playbook.repository

import com.playbook.domain.CreateTeamRequest
import com.playbook.domain.RejectTeamRequest
import com.playbook.domain.Team
import com.playbook.domain.TeamStatus
import com.playbook.domain.UpdateTeamRequest

interface TeamRepository {
    suspend fun listByClub(clubId: String, statuses: List<TeamStatus> = TeamStatus.entries): List<Team>
    suspend fun getById(teamId: String): Team?
    suspend fun create(clubId: String, request: CreateTeamRequest): Team
    suspend fun submitRequest(clubId: String, request: CreateTeamRequest, requestedByUserId: String): Team
    suspend fun update(teamId: String, request: UpdateTeamRequest): Team
    suspend fun setStatus(teamId: String, status: TeamStatus): Team
    suspend fun approve(teamId: String): Team
    suspend fun reject(teamId: String, request: RejectTeamRequest): Team
    suspend fun delete(teamId: String)
}
