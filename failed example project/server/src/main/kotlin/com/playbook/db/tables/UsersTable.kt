package com.playbook.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object UsersTable : Table("users") {
    val id = uuid("id").autoGenerate()
    val email = text("email")
    val displayName = text("display_name").nullable()
    val avatarUrl = text("avatar_url").nullable()
    val passwordHash = text("password_hash").nullable()
    val superAdmin = bool("super_admin").default(false)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}
