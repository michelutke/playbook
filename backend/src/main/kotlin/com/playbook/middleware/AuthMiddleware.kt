package com.playbook.middleware

import com.playbook.db.tables.ClubManagersTable
import com.playbook.db.tables.TeamMembershipsTable
import com.playbook.plugins.ForbiddenException
import com.playbook.plugins.userId
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

/**
 * TM-011: Verify the authenticated user is a ClubManager for the given club.
 * Checks club_managers table directly (DB per-request, no JWT caching).
 */
fun RoutingContext.requireClubManager(clubId: String): String {
    val uid = call.userId
    val isManager = transaction {
        ClubManagersTable.select {
            (ClubManagersTable.clubId eq UUID.fromString(clubId)) and
            (ClubManagersTable.userId eq UUID.fromString(uid))
        }.count() > 0
    }
    if (!isManager) throw ForbiddenException("Club manager access required")
    return uid
}

/**
 * TM-012: Verify the authenticated user is a Coach on the given team.
 * Checks team_memberships table directly.
 */
fun RoutingContext.requireCoachOnTeam(teamId: String): String {
    val uid = call.userId
    val isCoach = transaction {
        TeamMembershipsTable.select {
            (TeamMembershipsTable.teamId eq UUID.fromString(teamId)) and
            (TeamMembershipsTable.userId eq UUID.fromString(uid)) and
            (TeamMembershipsTable.role eq "coach")
        }.count() > 0
    }
    if (!isCoach) throw ForbiddenException("Coach access required for this team")
    return uid
}
