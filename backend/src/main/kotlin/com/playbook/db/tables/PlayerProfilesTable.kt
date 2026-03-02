package com.playbook.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object PlayerProfilesTable : Table("player_profiles") {
    val teamId = uuid("team_id").references(TeamsTable.id, onDelete = ReferenceOption.CASCADE)
    val userId = uuid("user_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val jerseyNumber = integer("jersey_number").nullable()
    val position = text("position").nullable()
    override val primaryKey = PrimaryKey(teamId, userId)
}
