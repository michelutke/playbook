package com.playbook.db.repositories

import com.playbook.db.tables.ClubsTable
import com.playbook.db.tables.InvitesTable
import com.playbook.db.tables.TeamMembershipsTable
import com.playbook.db.tables.TeamsTable
import com.playbook.domain.*
import com.playbook.infra.generateToken
import com.playbook.plugins.GoneException
import com.playbook.repository.InviteRepository
import io.ktor.server.config.*
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.simplejavamail.api.mailer.Mailer
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class InviteRepositoryImpl(
    private val mailer: Mailer,
    private val config: ApplicationConfig,
) : InviteRepository {

    override suspend fun create(teamId: String, request: CreateInviteRequest, invitedByUserId: String): Invite {
        val (invite, teamName) = newSuspendedTransaction {
            val tid = UUID.fromString(teamId)
            val team = TeamsTable.select { TeamsTable.id eq tid }.single()
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            val token = generateToken()
            val id = UUID.randomUUID()
            InvitesTable.insert {
                it[InvitesTable.id] = id
                it[InvitesTable.teamId] = tid
                it[inviteType] = if (request.role == MemberRole.COACH) "team_coach" else "team_player"
                it[role] = request.role.name.lowercase()
                it[invitedEmail] = request.email
                it[inviteToken] = token
                it[status] = "pending"
                it[invitedBy] = UUID.fromString(invitedByUserId)
                it[expiresAt] = now.plusDays(7)
                it[createdAt] = now
            }
            val invite = InvitesTable.select { InvitesTable.id eq id }.single().toInvite()
            Pair(invite, team[TeamsTable.name])
        }
        sendInviteEmail(invite, teamName)
        return invite
    }

    override suspend fun listPending(teamId: String): List<Invite> =
        newSuspendedTransaction {
            InvitesTable.select {
                (InvitesTable.teamId eq UUID.fromString(teamId)) and
                (InvitesTable.status eq "pending")
            }.map { it.toInvite() }
        }

    override suspend fun revoke(inviteId: String, teamId: String) =
        newSuspendedTransaction {
            InvitesTable.update({
                (InvitesTable.id eq UUID.fromString(inviteId)) and
                (InvitesTable.teamId eq UUID.fromString(teamId))
            }) {
                it[status] = "revoked"
            }
        }.let {}

    override suspend fun resolveToken(token: String): InviteContext? =
        newSuspendedTransaction {
            val row = (InvitesTable innerJoin TeamsTable innerJoin ClubsTable)
                .select { InvitesTable.inviteToken eq token }
                .singleOrNull() ?: return@newSuspendedTransaction null
            val invite = row.toInvite()
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            val expired = row[InvitesTable.expiresAt].isBefore(now)
            if (invite.status != InviteStatus.PENDING || expired) throw GoneException("Invite is no longer valid")
            InviteContext(
                invite = invite,
                teamName = row[TeamsTable.name],
                clubName = row[ClubsTable.name],
            )
        }

    override suspend fun accept(token: String, userId: String) =
        newSuspendedTransaction {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            val updated = InvitesTable.update({
                (InvitesTable.inviteToken eq token) and
                (InvitesTable.status eq "pending") and
                (InvitesTable.expiresAt greater now)
            }) {
                it[status] = "accepted"
                it[acceptedAt] = now
            }
            if (updated == 0) throw GoneException("Invite is no longer valid")
            val row = InvitesTable.select { InvitesTable.inviteToken eq token }.single()
            TeamMembershipsTable.insert {
                it[teamId] = row[InvitesTable.teamId]
                it[TeamMembershipsTable.userId] = UUID.fromString(userId)
                it[role] = row[InvitesTable.role]
                it[joinedAt] = now
            }
        }.let {}

    private fun sendInviteEmail(invite: Invite, teamName: String) {
        // Email sending is best-effort; logged on failure by caller
        val baseUrl = config.propertyOrNull("app.baseUrl")?.getString() ?: "https://app.playbook.example"
        val link = "$baseUrl/invites/${invite.inviteToken}"
        val fromAddress = config.propertyOrNull("smtp.fromAddress")?.getString() ?: "noreply@playbook.app"
        val fromName = config.propertyOrNull("smtp.fromName")?.getString() ?: "Playbook"
        com.playbook.email.sendInviteEmail(
            mailer = mailer,
            toEmail = invite.invitedEmail,
            teamName = teamName,
            roleName = invite.role.name.lowercase(),
            inviteLink = link,
            fromAddress = fromAddress,
            fromName = fromName,
        )
    }

    private fun ResultRow.toInvite() = Invite(
        id = this[InvitesTable.id].toString(),
        inviteType = if (this[InvitesTable.inviteType] == "team_coach") InviteType.TEAM_COACH else InviteType.TEAM_PLAYER,
        teamId = this[InvitesTable.teamId].toString(),
        role = if (this[InvitesTable.role] == "coach") MemberRole.COACH else MemberRole.PLAYER,
        invitedEmail = this[InvitesTable.invitedEmail],
        inviteToken = this[InvitesTable.inviteToken],
        status = when (this[InvitesTable.status]) {
            "accepted" -> InviteStatus.ACCEPTED
            "expired" -> InviteStatus.EXPIRED
            "revoked" -> InviteStatus.REVOKED
            else -> InviteStatus.PENDING
        },
        invitedBy = this[InvitesTable.invitedBy]?.toString(),
        expiresAt = this[InvitesTable.expiresAt].toInstant().toKotlinInstant(),
        createdAt = this[InvitesTable.createdAt].toInstant().toKotlinInstant(),
        acceptedAt = this[InvitesTable.acceptedAt]?.toInstant()?.toKotlinInstant(),
    )
}
