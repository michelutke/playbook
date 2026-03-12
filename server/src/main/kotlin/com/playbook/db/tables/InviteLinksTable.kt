package com.playbook.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.CustomFunction
import org.jetbrains.exposed.sql.UUIDColumnType
import org.jetbrains.exposed.sql.CurrentTimestamp

object InviteLinksTable : Table("invite_links") {
    val id = uuid("id").defaultExpression(CustomFunction("gen_random_uuid", UUIDColumnType()))
    val token = text("token").uniqueIndex().defaultExpression(CustomFunction("gen_random_uuid", UUIDColumnType()).castTo(org.jetbrains.exposed.sql.TextColumnType()))
    val teamId = uuid("team_id").references(TeamsTable.id, onDelete = ReferenceOption.CASCADE)
    val invitedByUserId = uuid("invited_by_user_id").references(UsersTable.id)
    val invitedEmail = text("invited_email").nullable()
    val role = text("role").default("player") // coach, player
    val expiresAt = timestamp("expires_at").defaultExpression(CustomFunction("NOW() + INTERVAL '7 days'", timestamp("expires_at").columnType))
    val redeemedAt = timestamp("redeemed_at").nullable()
    val redeemedByUserId = uuid("redeemed_by_user_id").references(UsersTable.id).nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(id)
}
