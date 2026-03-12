package com.playbook.db.repositories

import com.playbook.db.tables.ClubManagersTable
import com.playbook.db.tables.ClubsTable
import com.playbook.db.tables.UsersTable
import com.playbook.domain.Club
import com.playbook.domain.ClubStatus
import com.playbook.domain.CreateClubRequest
import com.playbook.domain.UpdateClubRequest
import com.playbook.repository.ClubRepository
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class ClubRepositoryImpl : ClubRepository {

    override suspend fun create(request: CreateClubRequest, createdByUserId: String): Club =
        newSuspendedTransaction {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            val id = UUID.randomUUID()
            ClubsTable.insert {
                it[ClubsTable.id] = id
                it[name] = request.name
                it[sportType] = request.sportType
                it[location] = request.location
                it[status] = "active"
                it[createdAt] = now
                it[updatedAt] = now
            }
            val creatorUuid = UUID.fromString(createdByUserId)
            val creatorEmail = UsersTable.selectAll()
                .where { UsersTable.id eq creatorUuid }
                .single()[UsersTable.email]
            ClubManagersTable.insert {
                it[clubId] = id
                it[userId] = creatorUuid
                it[invitedEmail] = creatorEmail
                it[status] = "active"
                it[addedBy] = creatorUuid
                it[addedAt] = now
                it[acceptedAt] = now
            }
            ClubsTable.selectAll().where { ClubsTable.id eq id }.single().toClub()
        }

    override suspend fun getById(clubId: String): Club? =
        newSuspendedTransaction {
            ClubsTable.selectAll().where { ClubsTable.id eq UUID.fromString(clubId) }.singleOrNull()?.toClub()
        }

    override suspend fun update(clubId: String, request: UpdateClubRequest): Club =
        newSuspendedTransaction {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            ClubsTable.update({ ClubsTable.id eq UUID.fromString(clubId) }) {
                request.name?.let { v -> it[name] = v }
                request.sportType?.let { v -> it[sportType] = v }
                request.location?.let { v -> it[location] = v }
                it[updatedAt] = now
            }
            ClubsTable.selectAll().where { ClubsTable.id eq UUID.fromString(clubId) }.single().toClub()
        }

    override suspend fun updateLogoUrl(clubId: String, logoUrl: String): Club =
        newSuspendedTransaction {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            ClubsTable.update({ ClubsTable.id eq UUID.fromString(clubId) }) {
                it[ClubsTable.logoUrl] = logoUrl
                it[updatedAt] = now
            }
            ClubsTable.selectAll().where { ClubsTable.id eq UUID.fromString(clubId) }.single().toClub()
        }

    override suspend fun uploadLogo(clubId: String, contentType: String, imageBytes: ByteArray): Club =
        throw UnsupportedOperationException("Logo upload is handled by the route layer on the backend")

    private fun ResultRow.toClub() = Club(
        id = this[ClubsTable.id].toString(),
        name = this[ClubsTable.name],
        logoUrl = this[ClubsTable.logoUrl],
        sportType = this[ClubsTable.sportType],
        location = this[ClubsTable.location],
        status = if (this[ClubsTable.status] == "active") ClubStatus.ACTIVE else ClubStatus.INACTIVE,
        createdAt = this[ClubsTable.createdAt].toInstant().toKotlinInstant(),
        updatedAt = this[ClubsTable.updatedAt].toInstant().toKotlinInstant(),
    )
}
