package com.playbook.domain.repositories

import com.playbook.domain.models.Club
import com.playbook.domain.models.Team
import java.util.UUID

interface ClubRepository {
    suspend fun create(name: String, sportType: String, location: String?, creatorUserId: UUID): Club
    suspend fun findById(id: UUID): Club?
    suspend fun update(id: UUID, name: String?, location: String?, logoPath: String?): Club
    suspend fun listTeams(clubId: UUID): List<Team>
    suspend fun hasRole(userId: UUID, clubId: UUID, role: String): Boolean
}
