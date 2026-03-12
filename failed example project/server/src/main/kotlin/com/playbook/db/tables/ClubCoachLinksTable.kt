package com.playbook.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object ClubCoachLinksTable : Table("club_coach_links") {
    val id = uuid("id").autoGenerate()
    val clubId = uuid("club_id").references(ClubsTable.id, onDelete = ReferenceOption.CASCADE)
    val token = text("token").uniqueIndex()
    val expiresAt = timestampWithTimeZone("expires_at")
    val createdBy = uuid("created_by").references(UsersTable.id).nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val revokedAt = timestampWithTimeZone("revoked_at").nullable()
    override val primaryKey = PrimaryKey(id)
}
