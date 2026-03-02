package com.playbook.db.repositories

import com.playbook.db.tables.PlayerProfilesTable
import com.playbook.db.tables.TeamMembershipsTable
import com.playbook.db.tables.UsersTable
import com.playbook.domain.MemberRole
import com.playbook.domain.PlayerProfile
import com.playbook.domain.RosterMember
import com.playbook.domain.UpdatePlayerProfileRequest
import com.playbook.plugins.ConflictException
import com.playbook.repository.MembershipRepository
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class MembershipRepositoryImpl : MembershipRepository {

    override suspend fun getRoster(teamId: String): List<RosterMember> =
        newSuspendedTransaction {
            val tid = UUID.fromString(teamId)
            (TeamMembershipsTable innerJoin UsersTable)
                .select { TeamMembershipsTable.teamId eq tid }
                .groupBy { it[TeamMembershipsTable.userId] }
                .map { (userId, rows) ->
                    val first = rows.first()
                    RosterMember(
                        userId = userId.toString(),
                        displayName = first[UsersTable.displayName],
                        avatarUrl = first[UsersTable.avatarUrl],
                        roles = rows.map { if (it[TeamMembershipsTable.role] == "coach") MemberRole.COACH else MemberRole.PLAYER },
                        joinedAt = rows.minOf { it[TeamMembershipsTable.joinedAt] }.toInstant().toKotlinInstant(),
                    )
                }
        }

    override suspend fun addRole(teamId: String, userId: String, role: MemberRole, addedByUserId: String) =
        newSuspendedTransaction {
            val tid = UUID.fromString(teamId)
            val uid = UUID.fromString(userId)
            val roleStr = role.name.lowercase()
            val exists = TeamMembershipsTable.select {
                (TeamMembershipsTable.teamId eq tid) and
                (TeamMembershipsTable.userId eq uid) and
                (TeamMembershipsTable.role eq roleStr)
            }.count() > 0
            if (exists) throw ConflictException("User already has role $role on this team")
            TeamMembershipsTable.insert {
                it[TeamMembershipsTable.teamId] = tid
                it[TeamMembershipsTable.userId] = uid
                it[TeamMembershipsTable.role] = roleStr
                it[addedBy] = UUID.fromString(addedByUserId)
                it[joinedAt] = OffsetDateTime.now(ZoneOffset.UTC)
            }
        }.let {}

    override suspend fun removeRole(teamId: String, userId: String, role: MemberRole) =
        newSuspendedTransaction {
            val tid = UUID.fromString(teamId)
            val uid = UUID.fromString(userId)
            val roleStr = role.name.lowercase()
            // Last-coach guard
            if (role == MemberRole.COACH) {
                val coachCount = TeamMembershipsTable.select {
                    (TeamMembershipsTable.teamId eq tid) and
                    (TeamMembershipsTable.role eq "coach")
                }.count()
                if (coachCount <= 1) throw ConflictException("Cannot remove the last coach from a team")
            }
            TeamMembershipsTable.deleteWhere {
                (teamId eq tid) and (this.userId eq uid) and (this.role eq roleStr)
            }
        }.let {}

    override suspend fun removeMember(teamId: String, userId: String) =
        newSuspendedTransaction {
            TeamMembershipsTable.deleteWhere {
                (teamId eq UUID.fromString(teamId)) and
                (this.userId eq UUID.fromString(userId))
            }
        }.let {}

    override suspend fun leaveTeam(teamId: String, userId: String) = removeMember(teamId, userId)

    override suspend fun getProfile(teamId: String, userId: String): PlayerProfile? =
        newSuspendedTransaction {
            PlayerProfilesTable.select {
                (PlayerProfilesTable.teamId eq UUID.fromString(teamId)) and
                (PlayerProfilesTable.userId eq UUID.fromString(userId))
            }.singleOrNull()?.toProfile()
        }

    override suspend fun updateProfile(teamId: String, userId: String, request: UpdatePlayerProfileRequest): PlayerProfile =
        newSuspendedTransaction {
            val tid = UUID.fromString(teamId)
            val uid = UUID.fromString(userId)
            PlayerProfilesTable.upsert {
                it[PlayerProfilesTable.teamId] = tid
                it[PlayerProfilesTable.userId] = uid
                request.jerseyNumber?.let { v -> it[jerseyNumber] = v }
                request.position?.let { v -> it[position] = v }
            }
            PlayerProfilesTable.select {
                (PlayerProfilesTable.teamId eq tid) and (PlayerProfilesTable.userId eq uid)
            }.single().toProfile()
        }

    override suspend fun hasRole(teamId: String, userId: String, role: MemberRole): Boolean =
        newSuspendedTransaction {
            TeamMembershipsTable.select {
                (TeamMembershipsTable.teamId eq UUID.fromString(teamId)) and
                (TeamMembershipsTable.userId eq UUID.fromString(userId)) and
                (TeamMembershipsTable.role eq role.name.lowercase())
            }.count() > 0
        }

    private fun ResultRow.toProfile() = PlayerProfile(
        teamId = this[PlayerProfilesTable.teamId].toString(),
        userId = this[PlayerProfilesTable.userId].toString(),
        jerseyNumber = this[PlayerProfilesTable.jerseyNumber],
        position = this[PlayerProfilesTable.position],
    )
}
