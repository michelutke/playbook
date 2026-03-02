package com.playbook.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object InvitesTable : Table("invites") {
    val id = uuid("id").autoGenerate()
    val inviteType = text("invite_type")
    val teamId = uuid("team_id").references(TeamsTable.id, onDelete = ReferenceOption.CASCADE)
    val role = text("role")
    val invitedEmail = text("invited_email")
    val inviteToken = text("invite_token").uniqueIndex()
    val status = text("status").default("pending")
    val invitedBy = uuid("invited_by").references(UsersTable.id).nullable()
    val expiresAt = timestampWithTimeZone("expires_at")
    val createdAt = timestampWithTimeZone("created_at")
    val acceptedAt = timestampWithTimeZone("accepted_at").nullable()
    override val primaryKey = PrimaryKey(id)
}
