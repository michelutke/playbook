package com.playbook.db.repositories

import com.playbook.db.tables.TeamsTable
import com.playbook.domain.*
import com.playbook.repository.TeamRepository
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class TeamRepositoryImpl : TeamRepository {

    override suspend fun listByClub(clubId: String, statuses: List<TeamStatus>): List<Team> =
        newSuspendedTransaction {
            val statusStrings = statuses.map { it.name.lowercase() }
            TeamsTable.select {
                (TeamsTable.clubId eq UUID.fromString(clubId)) and
                (TeamsTable.status inList statusStrings)
            }.map { it.toTeam() }
        }

    override suspend fun getById(teamId: String): Team? =
        newSuspendedTransaction {
            TeamsTable.select { TeamsTable.id eq UUID.fromString(teamId) }.singleOrNull()?.toTeam()
        }

    override suspend fun create(clubId: String, request: CreateTeamRequest): Team =
        newSuspendedTransaction {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            val id = UUID.randomUUID()
            TeamsTable.insert {
                it[TeamsTable.id] = id
                it[TeamsTable.clubId] = UUID.fromString(clubId)
                it[name] = request.name
                it[description] = request.description
                it[status] = "active"
                it[createdAt] = now
                it[updatedAt] = now
            }
            TeamsTable.select { TeamsTable.id eq id }.single().toTeam()
        }

    override suspend fun submitRequest(clubId: String, request: CreateTeamRequest, requestedByUserId: String): Team =
        newSuspendedTransaction {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            val id = UUID.randomUUID()
            TeamsTable.insert {
                it[TeamsTable.id] = id
                it[TeamsTable.clubId] = UUID.fromString(clubId)
                it[name] = request.name
                it[description] = request.description
                it[status] = "pending"
                it[requestedBy] = UUID.fromString(requestedByUserId)
                it[createdAt] = now
                it[updatedAt] = now
            }
            TeamsTable.select { TeamsTable.id eq id }.single().toTeam()
        }

    override suspend fun update(teamId: String, request: UpdateTeamRequest): Team =
        newSuspendedTransaction {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            TeamsTable.update({ TeamsTable.id eq UUID.fromString(teamId) }) {
                request.name?.let { v -> it[name] = v }
                request.description?.let { v -> it[description] = v }
                it[updatedAt] = now
            }
            TeamsTable.select { TeamsTable.id eq UUID.fromString(teamId) }.single().toTeam()
        }

    override suspend fun setStatus(teamId: String, status: TeamStatus): Team =
        newSuspendedTransaction {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            TeamsTable.update({ TeamsTable.id eq UUID.fromString(teamId) }) {
                it[TeamsTable.status] = status.name.lowercase()
                it[updatedAt] = now
            }
            TeamsTable.select { TeamsTable.id eq UUID.fromString(teamId) }.single().toTeam()
        }

    override suspend fun approve(teamId: String): Team = setStatus(teamId, TeamStatus.ACTIVE)

    override suspend fun reject(teamId: String, request: RejectTeamRequest): Team =
        newSuspendedTransaction {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            TeamsTable.update({ TeamsTable.id eq UUID.fromString(teamId) }) {
                it[status] = "archived"
                it[rejectionReason] = request.reason
                it[updatedAt] = now
            }
            TeamsTable.select { TeamsTable.id eq UUID.fromString(teamId) }.single().toTeam()
        }

    override suspend fun delete(teamId: String) =
        newSuspendedTransaction {
            TeamsTable.deleteWhere { id eq UUID.fromString(teamId) }
        }.let {}

    private fun ResultRow.toTeam() = Team(
        id = this[TeamsTable.id].toString(),
        clubId = this[TeamsTable.clubId].toString(),
        name = this[TeamsTable.name],
        description = this[TeamsTable.description],
        status = when (this[TeamsTable.status]) {
            "active" -> TeamStatus.ACTIVE
            "archived" -> TeamStatus.ARCHIVED
            else -> TeamStatus.PENDING
        },
        requestedBy = this[TeamsTable.requestedBy]?.toString(),
        rejectionReason = this[TeamsTable.rejectionReason],
        createdAt = this[TeamsTable.createdAt].toInstant().toKotlinInstant(),
        updatedAt = this[TeamsTable.updatedAt].toInstant().toKotlinInstant(),
    )
}
