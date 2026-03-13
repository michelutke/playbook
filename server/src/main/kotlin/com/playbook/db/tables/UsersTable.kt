package com.playbook.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object UsersTable : Table("users") {
    val id = uuid("id").defaultExpression(org.jetbrains.exposed.sql.CustomFunction("gen_random_uuid", org.jetbrains.exposed.sql.UUIDColumnType()))
    val email = text("email").uniqueIndex()
    val passwordHash = text("password_hash")
    val displayName = text("display_name")
    val avatarUrl = text("avatar_url").nullable()
    val isSuperAdmin = bool("is_super_admin").default(false)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(id)
}
