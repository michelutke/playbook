package ch.teamorg.fake

import ch.teamorg.domain.Club
import ch.teamorg.domain.Team
import ch.teamorg.repository.ClubRepository

class FakeClubRepository : ClubRepository {

    var createClubResult: Result<Club> = Result.success(
        Club(id = "club1", name = "Test Club", logoUrl = null, sportType = "volleyball")
    )
    var uploadLogoResult: Result<Club> = Result.success(
        Club(id = "club1", name = "Test Club", logoUrl = "https://example.com/logo.png", sportType = "volleyball")
    )
    var getClubTeamsResult: Result<List<Team>> = Result.success(emptyList())

    var lastCreatedName: String? = null
    var lastCreatedSportType: String? = null
    var lastCreatedLocation: String? = null
    var lastUploadedClubId: String? = null
    var lastUploadedExtension: String? = null

    fun reset() {
        createClubResult = Result.success(
            Club(id = "club1", name = "Test Club", logoUrl = null, sportType = "volleyball")
        )
        uploadLogoResult = Result.success(
            Club(id = "club1", name = "Test Club", logoUrl = "https://example.com/logo.png", sportType = "volleyball")
        )
        getClubTeamsResult = Result.success(emptyList())
        lastCreatedName = null
        lastCreatedSportType = null
        lastCreatedLocation = null
        lastUploadedClubId = null
        lastUploadedExtension = null
    }

    override suspend fun createClub(name: String, sportType: String, location: String?): Result<Club> {
        lastCreatedName = name
        lastCreatedSportType = sportType
        lastCreatedLocation = location
        return createClubResult
    }

    override suspend fun uploadLogo(clubId: String, imageBytes: ByteArray, extension: String): Result<Club> {
        lastUploadedClubId = clubId
        lastUploadedExtension = extension
        return uploadLogoResult
    }

    override suspend fun getClubTeams(clubId: String): Result<List<Team>> = getClubTeamsResult
}
