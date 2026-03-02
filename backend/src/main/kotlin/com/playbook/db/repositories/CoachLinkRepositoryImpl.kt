package com.playbook.db.repositories

import com.playbook.db.tables.ClubCoachLinksTable
import com.playbook.db.tables.ClubsTable
import com.playbook.db.tables.TeamMembershipsTable
import com.playbook.db.tables.TeamsTable
import com.playbook.domain.CoachLink
import com.playbook.domain.CoachLinkContext
import com.playbook.domain.CreateCoachLinkRequest
import com.playbook.domain.TeamStatus
import com.playbook.infra.generateToken
import com.playbook.plugins.GoneException
import com.playbook.repository.CoachLinkRepository
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class CoachLinkRepositoryImpl : CoachLinkRepository {

    override suspend fun getActive(clubId: String): CoachLink? =
        newSuspendedTransaction {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            ClubCoachLinksTable.select {
                (ClubCoachLinksTable.clubId eq UUID.fromString(clubId)) and
                ClubCoachLinksTable.revokedAt.isNull() and
                (ClubCoachLinksTable.expiresAt greater now)
            }.singleOrNull()?.toLink()
        }

    override suspend fun rotate(clubId: String, request: CreateCoachLinkRequest, createdByUserId: String): CoachLink =
        newSuspendedTransaction {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            val cid = UUID.fromString(clubId)
            // Revoke existing active link
            ClubCoachLinksTable.update({
                (ClubCoachLinksTable.clubId eq cid) and ClubCoachLinksTable.revokedAt.isNull()
            }) {
                it[revokedAt] = now
            }
            // Create new link
            val id = UUID.randomUUID()
            val expiresAt = now.plusDays(request.expiresInDays.toLong())
            ClubCoachLinksTable.insert {
                it[ClubCoachLinksTable.id] = id
                it[ClubCoachLinksTable.clubId] = cid
                it[token] = generateToken()
                it[ClubCoachLinksTable.expiresAt] = expiresAt
                it[createdBy] = UUID.fromString(createdByUserId)
                it[createdAt] = now
            }
            ClubCoachLinksTable.select { ClubCoachLinksTable.id eq id }.single().toLink()
        }

    override suspend fun revoke(clubId: String) =
        newSuspendedTransaction {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            ClubCoachLinksTable.update({
                (ClubCoachLinksTable.clubId eq UUID.fromString(clubId)) and
                ClubCoachLinksTable.revokedAt.isNull()
            }) {
                it[revokedAt] = now
            }
        }.let {}

    override suspend fun resolveToken(token: String): CoachLinkContext? =
        newSuspendedTransaction {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            val row = (ClubCoachLinksTable innerJoin ClubsTable)
                .select { ClubCoachLinksTable.token eq token }
                .singleOrNull() ?: return@newSuspendedTransaction null
            val link = row.toLink()
            if (link.revokedAt != null || row[ClubCoachLinksTable.expiresAt].isBefore(now)) {
                throw GoneException("Coach link is no longer valid")
            }
            CoachLinkContext(
                coachLink = link,
                clubName = row[ClubsTable.name],
                clubSportType = row[ClubsTable.sportType],
            )
        }

    override suspend fun join(token: String, userId: String) =
        newSuspendedTransaction {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            val row = ClubCoachLinksTable.select { ClubCoachLinksTable.token eq token }.singleOrNull()
                ?: throw GoneException("Coach link not found")
            if (row[ClubCoachLinksTable.revokedAt] != null || row[ClubCoachLinksTable.expiresAt].isBefore(now)) {
                throw GoneException("Coach link is no longer valid")
            }
            // Find or create a pending team for this coach.
            // uq_pending_team_per_coach (V10 migration) enforces at most one pending team
            // per coach per club at the DB level, making concurrent joins idempotent.
            val clubId = row[ClubCoachLinksTable.clubId]
            val pendingTeam = TeamsTable.select {
                (TeamsTable.clubId eq clubId) and
                (TeamsTable.status eq "pending") and
                (TeamsTable.requestedBy eq UUID.fromString(userId))
            }.singleOrNull()
            if (pendingTeam == null) {
                val teamId = UUID.randomUUID()
                try {
                    // Team name defaults to "My Team" — coach should rename via PATCH /teams/{id}
                    TeamsTable.insert {
                        it[id] = teamId
                        it[TeamsTable.clubId] = clubId
                        it[name] = "My Team"
                        it[status] = "pending"
                        it[requestedBy] = UUID.fromString(userId)
                        it[createdAt] = now
                        it[updatedAt] = now
                    }
                    TeamMembershipsTable.insert {
                        it[teamId] = teamId
                        it[TeamMembershipsTable.userId] = UUID.fromString(userId)
                        it[role] = "coach"
                        it[joinedAt] = now
                    }
                } catch (e: ExposedSQLException) {
                    if (e.sqlState == "23505") {
                        // Concurrent join already created the pending team — idempotent, nothing to do
                    } else throw e
                }
            }
        }.let {}

    private fun ResultRow.toLink() = CoachLink(
        id = this[ClubCoachLinksTable.id].toString(),
        clubId = this[ClubCoachLinksTable.clubId].toString(),
        token = this[ClubCoachLinksTable.token],
        expiresAt = this[ClubCoachLinksTable.expiresAt].toInstant().toKotlinInstant(),
        createdBy = this[ClubCoachLinksTable.createdBy]?.toString(),
        createdAt = this[ClubCoachLinksTable.createdAt].toInstant().toKotlinInstant(),
        revokedAt = this[ClubCoachLinksTable.revokedAt]?.toInstant()?.toKotlinInstant(),
    )
}
