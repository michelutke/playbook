package com.playbook.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

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
