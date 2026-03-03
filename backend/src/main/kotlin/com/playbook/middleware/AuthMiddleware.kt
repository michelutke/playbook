package com.playbook.middleware

import com.playbook.db.tables.ClubManagersTable
import com.playbook.db.tables.EventTeamsTable
import com.playbook.db.tables.SubgroupsTable
import com.playbook.db.tables.TeamMembershipsTable
import com.playbook.plugins.ForbiddenException
import com.playbook.plugins.userId
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

/**
 * TM-011: Verify the authenticated user is a ClubManager for the given club.
 * Checks club_managers table directly (DB per-request, no JWT caching).
 */
suspend fun RoutingContext.requireClubManager(clubId: String): String {
    val uid = call.userId
    val isManager = newSuspendedTransaction {
        ClubManagersTable.selectAll().where {
            (ClubManagersTable.clubId eq UUID.fromString(clubId)) and
            (ClubManagersTable.userId eq UUID.fromString(uid)) and
            (ClubManagersTable.status eq "active")
        }.count() > 0
    }
    if (!isManager) throw ForbiddenException("Club manager access required")
    return uid
}

/**
 * TM-012: Verify the authenticated user is a Coach on the given team.
 * Checks team_memberships table directly.
 */
suspend fun RoutingContext.requireCoachOnTeam(teamId: String): String {
    val uid = call.userId
    val isCoach = newSuspendedTransaction {
        TeamMembershipsTable.selectAll().where {
            (TeamMembershipsTable.teamId eq UUID.fromString(teamId)) and
            (TeamMembershipsTable.userId eq UUID.fromString(uid)) and
            (TeamMembershipsTable.role eq "coach")
        }.count() > 0
    }
    if (!isCoach) throw ForbiddenException("Coach access required for this team")
    return uid
}

/**
 * ES: Verify the authenticated user is a Coach on any team targeting this event.
 */
suspend fun RoutingContext.requireCoachOnEventTeam(eventId: String): String {
    val uid = call.userId
    val isCoach = newSuspendedTransaction {
        EventTeamsTable
            .join(TeamMembershipsTable, JoinType.INNER, EventTeamsTable.teamId, TeamMembershipsTable.teamId)
            .selectAll().where {
                (EventTeamsTable.eventId eq UUID.fromString(eventId)) and
                (TeamMembershipsTable.userId eq UUID.fromString(uid)) and
                (TeamMembershipsTable.role eq "coach")
            }.count() > 0
    }
    if (!isCoach) throw ForbiddenException("Coach access required for this event")
    return uid
}

/**
 * ES: Verify the authenticated user is a Coach on the team that owns this subgroup.
 */
suspend fun RoutingContext.requireCoachOnSubgroupTeam(subgroupId: String): String {
    val uid = call.userId
    val teamId = newSuspendedTransaction {
        SubgroupsTable.selectAll().where { SubgroupsTable.id eq UUID.fromString(subgroupId) }
            .singleOrNull()?.get(SubgroupsTable.teamId)
    } ?: throw ForbiddenException("Subgroup not found")
    return requireCoachOnTeam(teamId.toString())
}

/**
 * C-1: Verify the authenticated user is a member (any role) of any team that owns this event.
 */
suspend fun RoutingContext.requireMemberOnEventTeam(eventId: String): String {
    val uid = call.userId
    val isMember = newSuspendedTransaction {
        EventTeamsTable
            .join(TeamMembershipsTable, JoinType.INNER, EventTeamsTable.teamId, TeamMembershipsTable.teamId)
            .selectAll().where {
                (EventTeamsTable.eventId eq UUID.fromString(eventId)) and
                (TeamMembershipsTable.userId eq UUID.fromString(uid))
            }.count() > 0
    }
    if (!isMember) throw ForbiddenException("Team membership required for this event")
    return uid
}

/**
 * C-2: Verify the caller is either the target user, or a coach on at least one team shared with the target.
 */
suspend fun RoutingContext.requireSelfOrSharedCoach(targetUserId: String): String {
    val uid = call.userId
    if (uid == targetUserId) return uid
    val isCoach = newSuspendedTransaction {
        val targetUid = UUID.fromString(targetUserId)
        val callerTeams = TeamMembershipsTable.selectAll()
            .where {
                (TeamMembershipsTable.userId eq UUID.fromString(uid)) and
                (TeamMembershipsTable.role eq "coach")
            }.map { it[TeamMembershipsTable.teamId] }
        if (callerTeams.isEmpty()) return@newSuspendedTransaction false
        TeamMembershipsTable.selectAll()
            .where {
                (TeamMembershipsTable.userId eq targetUid) and
                (TeamMembershipsTable.teamId inList callerTeams)
            }.count() > 0
    }
    if (!isCoach) throw ForbiddenException("Access denied")
    return uid
}
