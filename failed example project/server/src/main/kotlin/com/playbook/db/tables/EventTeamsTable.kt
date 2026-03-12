package com.playbook.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object EventTeamsTable : Table("event_teams") {
    val eventId = uuid("event_id").references(EventsTable.id, onDelete = ReferenceOption.CASCADE)
    val teamId = uuid("team_id").references(TeamsTable.id, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(eventId, teamId)
}
