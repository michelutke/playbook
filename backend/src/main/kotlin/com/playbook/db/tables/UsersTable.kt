package com.playbook.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object UsersTable : Table("users") {
    val id = uuid("id").autoGenerate()
    val email = text("email")
    val displayName = text("display_name").nullable()
    val avatarUrl = text("avatar_url").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}
