package ch.teamorg.repository

import ch.teamorg.domain.Club
import ch.teamorg.domain.Team

interface ClubRepository {
    suspend fun createClub(name: String, sportType: String, location: String?): Result<Club>
    suspend fun uploadLogo(clubId: String, imageBytes: ByteArray, extension: String): Result<Club>
    suspend fun getClubTeams(clubId: String): Result<List<Team>>
}
