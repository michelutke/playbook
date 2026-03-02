package com.playbook.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object ClubManagersTable : Table("club_managers") {
    val clubId = uuid("club_id").references(ClubsTable.id, onDelete = ReferenceOption.CASCADE)
    val userId = uuid("user_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val addedBy = uuid("added_by").references(UsersTable.id).nullable()
    val addedAt = timestampWithTimeZone("added_at")
    override val primaryKey = PrimaryKey(clubId, userId)
}
