package com.playbook.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

/**
 * SA-010/SA-011: Immutable audit trail for all super-admin mutations.
 *
 * Every non-GET request under /sa/* is recorded by [AuditPlugin].
 * When a super-admin impersonates a manager, [actorId] holds the SA's user ID and
 * [impersonatedAs] holds the manager's user ID — making the real actor always traceable.
 *
 * Column notes:
 * - [action]   — verb string, e.g. "PATCH /sa/clubs/{id}"
 * - [payload]  — JSON snapshot of the request body (nullable; omitted for sensitive ops)
 * - [impersonatedAs] / [impersonationSessionId] — null unless the request was made
 *   under an active impersonation session
 */
object AuditLogTable : Table("audit_log") {
    val id = uuid("id").autoGenerate()
    val actorId = uuid("actor_id").references(UsersTable.id)
    val action = text("action")
    val targetType = text("target_type").nullable()
    val targetId = uuid("target_id").nullable()
    val payload = text("payload").nullable()
    val impersonatedAs = uuid("impersonated_as").references(UsersTable.id).nullable()
    val impersonationSessionId = uuid("impersonation_session_id").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    override val primaryKey = PrimaryKey(id)
}
