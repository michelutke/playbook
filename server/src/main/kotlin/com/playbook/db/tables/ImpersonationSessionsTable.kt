package com.playbook.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

/**
 * SA-008/SA-009: Tracks super-admin impersonation sessions.
 *
 * Lifecycle: created on POST /sa/clubs/{id}/managers/{id}/impersonate,
 * closed (endedAt set) on POST /sa/impersonation/{sessionId}/end.
 *
 * Column notes:
 * - [superadminId] — the SA who opened the session (used for ownership check on end)
 * - [managerId]    — the club manager being impersonated
 * - [expiresAt]    — hard cap of 1 hour from creation; Ktor JWT verifier enforces this
 * - [endedAt]      — null while session is active; set to close early
 */
object ImpersonationSessionsTable : Table("impersonation_sessions") {
    val id = uuid("id").autoGenerate()
    val superadminId = uuid("superadmin_id").references(UsersTable.id)
    val managerId = uuid("manager_id").references(UsersTable.id)
    val clubId = uuid("club_id").references(ClubsTable.id)
    val startedAt = timestampWithTimeZone("started_at")
    val expiresAt = timestampWithTimeZone("expires_at")
    val endedAt = timestampWithTimeZone("ended_at").nullable()
    override val primaryKey = PrimaryKey(id)
}
