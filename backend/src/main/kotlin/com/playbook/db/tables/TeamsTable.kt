package com.playbook.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object TeamsTable : Table("teams") {
    val id = uuid("id").autoGenerate()
    val clubId = uuid("club_id").references(ClubsTable.id, onDelete = ReferenceOption.CASCADE)
    val name = text("name")
    val description = text("description").nullable()
    val status = text("status").default("pending")
    val requestedBy = uuid("requested_by").references(UsersTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val rejectionReason = text("rejection_reason").nullable()
    val checkInEnabled = bool("check_in_enabled").default(false)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}
