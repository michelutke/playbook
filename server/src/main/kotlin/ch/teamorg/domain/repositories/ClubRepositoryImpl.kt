package ch.teamorg.domain.repositories

import ch.teamorg.db.tables.ClubRolesTable
import ch.teamorg.db.tables.ClubsTable
import ch.teamorg.db.tables.TeamsTable
import ch.teamorg.domain.models.Club
import ch.teamorg.domain.models.Team
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ClubRepositoryImpl : ClubRepository {
    override suspend fun create(name: String, sportType: String, location: String?, creatorUserId: UUID): Club = transaction {
        val clubId = ClubsTable.insert {
            it[ClubsTable.name] = name
            it[ClubsTable.sportType] = sportType
            it[ClubsTable.location] = location
        } get ClubsTable.id

        ClubRolesTable.insert {
            it[ClubRolesTable.clubId] = clubId
            it[ClubRolesTable.userId] = creatorUserId
            it[ClubRolesTable.role] = "club_manager"
        }

        ClubsTable.selectAll().where { ClubsTable.id eq clubId }
            .map(::rowToClub)
            .single()
    }

    override suspend fun findById(id: UUID): Club? = transaction {
        ClubsTable.selectAll().where { ClubsTable.id eq id }
            .map(::rowToClub)
            .singleOrNull()
    }

    override suspend fun update(id: UUID, name: String?, location: String?, logoUrl: String?): Club = transaction {
        ClubsTable.update({ ClubsTable.id eq id }) {
            if (name != null) it[ClubsTable.name] = name
            if (location != null) it[ClubsTable.location] = location
            if (logoUrl != null) it[ClubsTable.logoPath] = logoUrl
            it[ClubsTable.updatedAt] = java.time.Instant.now()
        }

        ClubsTable.selectAll().where { ClubsTable.id eq id }
            .map(::rowToClub)
            .single()
    }

    override suspend fun listTeams(clubId: UUID): List<Team> = transaction {
        TeamsTable.selectAll().where { (TeamsTable.clubId eq clubId) and TeamsTable.archivedAt.isNull() }
            .map(::rowToTeam)
    }

    override suspend fun hasRole(userId: UUID, clubId: UUID, role: String): Boolean = transaction {
        !ClubRolesTable.selectAll().where {
            (ClubRolesTable.userId eq userId) and
            (ClubRolesTable.clubId eq clubId) and
            (ClubRolesTable.role eq role)
        }.empty()
    }

    private fun rowToClub(row: ResultRow) = Club(
        id = row[ClubsTable.id].toString(),
        name = row[ClubsTable.name],
        sportType = row[ClubsTable.sportType],
        location = row[ClubsTable.location],
        logoUrl = row[ClubsTable.logoPath],
        createdAt = row[ClubsTable.createdAt].toString(),
        updatedAt = row[ClubsTable.updatedAt].toString()
    )

    private fun rowToTeam(row: ResultRow) = Team(
        id = row[TeamsTable.id].toString(),
        clubId = row[TeamsTable.clubId].toString(),
        name = row[TeamsTable.name],
        description = row[TeamsTable.description],
        archivedAt = row[TeamsTable.archivedAt]?.toString(),
        createdAt = row[TeamsTable.createdAt].toString(),
        updatedAt = row[TeamsTable.updatedAt].toString()
    )
}
