package ch.teamorg.domain.repositories

import ch.teamorg.db.tables.ClubRolesTable
import ch.teamorg.db.tables.TeamRolesTable
import ch.teamorg.db.tables.TeamsTable
import ch.teamorg.db.tables.UsersTable
import ch.teamorg.domain.models.Team
import ch.teamorg.domain.models.TeamMember
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class TeamRepositoryImpl : TeamRepository {
    override suspend fun create(clubId: UUID, name: String, description: String?): Team = transaction {
        val teamId = TeamsTable.insert {
            it[TeamsTable.clubId] = clubId
            it[TeamsTable.name] = name
            it[TeamsTable.description] = description
        } get TeamsTable.id

        TeamsTable.selectAll().where { TeamsTable.id eq teamId }
            .map(::rowToTeam)
            .single()
    }

    override suspend fun findById(id: UUID): Team? = transaction {
        TeamsTable.selectAll().where { TeamsTable.id eq id }
            .map(::rowToTeam)
            .singleOrNull()
    }

    override suspend fun update(id: UUID, name: String?, description: String?): Team = transaction {
        TeamsTable.update({ TeamsTable.id eq id }) {
            if (name != null) it[TeamsTable.name] = name
            if (description != null) it[TeamsTable.description] = description
            it[TeamsTable.updatedAt] = java.time.Instant.now()
        }

        TeamsTable.selectAll().where { TeamsTable.id eq id }
            .map(::rowToTeam)
            .single()
    }

    override suspend fun archive(id: UUID): Team = transaction {
        TeamsTable.update({ TeamsTable.id eq id }) {
            it[TeamsTable.archivedAt] = java.time.Instant.now()
            it[TeamsTable.updatedAt] = java.time.Instant.now()
        }

        TeamsTable.selectAll().where { TeamsTable.id eq id }
            .map(::rowToTeam)
            .single()
    }

    override suspend fun listMembers(teamId: UUID): List<TeamMember> = transaction {
        (TeamRolesTable innerJoin UsersTable).selectAll().where { TeamRolesTable.teamId eq teamId }
            .map { row ->
                TeamMember(
                    userId = row[UsersTable.id].toString(),
                    displayName = row[UsersTable.displayName],
                    avatarUrl = row[UsersTable.avatarUrl],
                    role = row[TeamRolesTable.role],
                    jerseyNumber = row[TeamRolesTable.jerseyNumber],
                    position = row[TeamRolesTable.position]
                )
            }
    }

    override suspend fun hasRole(userId: UUID, teamId: UUID, vararg roles: String): Boolean = transaction {
        // 1. Check team roles
        val hasTeamRole = !TeamRolesTable.selectAll().where {
            (TeamRolesTable.userId eq userId) and
            (TeamRolesTable.teamId eq teamId) and
            (TeamRolesTable.role inList roles.toList())
        }.empty()

        if (hasTeamRole) return@transaction true

        // 2. Check if user is ClubManager of the club this team belongs to
        val clubId = TeamsTable.select(TeamsTable.clubId).where { TeamsTable.id eq teamId }
            .map { it[TeamsTable.clubId] }
            .singleOrNull()

        if (clubId != null) {
            return@transaction !ClubRolesTable.selectAll().where {
                (ClubRolesTable.userId eq userId) and
                (ClubRolesTable.clubId eq clubId) and
                (ClubRolesTable.role eq "club_manager")
            }.empty()
        }

        false
    }

    override suspend fun getClubId(teamId: UUID): UUID? = transaction {
        TeamsTable.select(TeamsTable.clubId).where { TeamsTable.id eq teamId }
            .map { it[TeamsTable.clubId] }
            .singleOrNull()
    }

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
