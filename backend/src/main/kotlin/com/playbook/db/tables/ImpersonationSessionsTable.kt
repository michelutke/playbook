package com.playbook.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

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
