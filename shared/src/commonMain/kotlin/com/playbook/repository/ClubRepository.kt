package com.playbook.repository

import com.playbook.domain.Club
import com.playbook.domain.Team

interface ClubRepository {
    suspend fun createClub(name: String, sportType: String, location: String?): Result<Club>
    suspend fun uploadLogo(clubId: String, imageBytes: ByteArray, extension: String): Result<Club>
    suspend fun getClubTeams(clubId: String): Result<List<Team>>
}
