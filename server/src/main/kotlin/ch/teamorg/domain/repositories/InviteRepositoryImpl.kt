package ch.teamorg.domain.repositories

import ch.teamorg.db.tables.*
import ch.teamorg.domain.models.InviteDetails
import ch.teamorg.domain.models.InviteLink
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

class InviteRepositoryImpl : InviteRepository {
    override suspend fun create(teamId: UUID, createdByUserId: UUID, role: String, email: String?): InviteLink = transaction {
        val insertedId = InviteLinksTable.insert {
            it[InviteLinksTable.token] = UUID.randomUUID().toString()
            it[InviteLinksTable.teamId] = teamId
            it[InviteLinksTable.invitedByUserId] = createdByUserId
            it[InviteLinksTable.role] = role
            it[InviteLinksTable.invitedEmail] = email
            it[InviteLinksTable.expiresAt] = Instant.now().plusSeconds(7 * 24 * 60 * 60)
        } get InviteLinksTable.id

        InviteLinksTable.selectAll().where { InviteLinksTable.id eq insertedId }
            .map(::rowToInviteLink)
            .single()
    }

    override suspend fun findByToken(token: String): InviteLink? = transaction {
        InviteLinksTable.selectAll().where { InviteLinksTable.token eq token }
            .map(::rowToInviteLink)
            .singleOrNull()
    }

    override suspend fun getInviteDetails(token: String): InviteDetails? = transaction {
        (InviteLinksTable innerJoin TeamsTable innerJoin ClubsTable)
            .join(UsersTable, JoinType.INNER, InviteLinksTable.invitedByUserId, UsersTable.id)
            .select(InviteLinksTable.token, TeamsTable.name, ClubsTable.name, InviteLinksTable.role, UsersTable.displayName, InviteLinksTable.expiresAt, InviteLinksTable.redeemedAt)
            .where { InviteLinksTable.token eq token }
            .map {
                InviteDetails(
                    token = it[InviteLinksTable.token],
                    teamName = it[TeamsTable.name],
                    clubName = it[ClubsTable.name],
                    role = it[InviteLinksTable.role],
                    invitedBy = it[UsersTable.displayName],
                    expiresAt = it[InviteLinksTable.expiresAt].toString(),
                    alreadyRedeemed = it[InviteLinksTable.redeemedAt] != null
                )
            }.singleOrNull()
    }

    override suspend fun redeem(token: String, userId: UUID): InviteLink = transaction {
        val invite = InviteLinksTable.selectAll().where { InviteLinksTable.token eq token }
            .singleOrNull() ?: throw NoSuchElementException("Invite not found")

        val teamId = invite[InviteLinksTable.teamId]
        val role = invite[InviteLinksTable.role]

        if (checkIsMember(teamId, userId, role)) {
            return@transaction rowToInviteLink(InviteLinksTable.selectAll().where { InviteLinksTable.token eq token }.single())
        }

        // Atomic: Mark redeemed + create team_role
        InviteLinksTable.update({ InviteLinksTable.token eq token }) {
            it[redeemedAt] = Instant.now()
            it[redeemedByUserId] = userId
        }

        TeamRolesTable.insert {
            it[TeamRolesTable.userId] = userId
            it[TeamRolesTable.teamId] = teamId
            it[TeamRolesTable.role] = role
        }

        rowToInviteLink(InviteLinksTable.selectAll().where { InviteLinksTable.token eq token }.single())
    }

    override suspend fun listByTeam(teamId: UUID): List<InviteLink> = transaction {
        InviteLinksTable.selectAll().where { InviteLinksTable.teamId eq teamId }
            .orderBy(InviteLinksTable.createdAt to SortOrder.DESC)
            .map(::rowToInviteLink)
    }

    override suspend fun isMember(teamId: UUID, userId: UUID, role: String): Boolean = transaction {
        checkIsMember(teamId, userId, role)
    }

    // Non-suspend version for use inside transaction blocks
    private fun checkIsMember(teamId: UUID, userId: UUID, role: String): Boolean =
        !TeamRolesTable.selectAll().where {
            (TeamRolesTable.teamId eq teamId) and (TeamRolesTable.userId eq userId) and (TeamRolesTable.role eq role)
        }.empty()

    private fun rowToInviteLink(row: ResultRow) = InviteLink(
        id = row[InviteLinksTable.id].toString(),
        token = row[InviteLinksTable.token],
        teamId = row[InviteLinksTable.teamId].toString(),
        invitedByUserId = row[InviteLinksTable.invitedByUserId].toString(),
        invitedEmail = row[InviteLinksTable.invitedEmail],
        role = row[InviteLinksTable.role],
        expiresAt = row[InviteLinksTable.expiresAt].toString(),
        redeemedAt = row[InviteLinksTable.redeemedAt]?.toString(),
        redeemedByUserId = row[InviteLinksTable.redeemedByUserId]?.toString(),
        createdAt = row[InviteLinksTable.createdAt].toString()
    )
}
