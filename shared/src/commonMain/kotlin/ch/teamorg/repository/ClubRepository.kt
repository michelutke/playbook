package ch.teamorg.repository

import ch.teamorg.domain.Club
import ch.teamorg.domain.Team

interface ClubRepository {
    suspend fun createClub(name: String, sportType: String, location: String?): Result<Club>
    suspend fun uploadLogo(clubId: String, imageBytes: ByteArray, extension: String): Result<Club>
    suspend fun getClubTeams(clubId: String): Result<List<Team>>
    suspend fun createTeam(clubId: String, name: String, description: String?): Result<Team>
    suspend fun updateTeam(teamId: String, name: String?, description: String?): Result<Team>
}
