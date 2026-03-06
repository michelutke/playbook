package com.playbook.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object TeamMembershipsTable : Table("team_memberships") {
    val id = uuid("id").autoGenerate()
    val teamId = uuid("team_id").references(TeamsTable.id, onDelete = ReferenceOption.CASCADE)
    val userId = uuid("user_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val role = text("role")
    val addedBy = uuid("added_by").references(UsersTable.id).nullable()
    val joinedAt = timestampWithTimeZone("joined_at")
    override val primaryKey = PrimaryKey(id)
}
