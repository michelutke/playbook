package com.playbook.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object EventSubgroupsTable : Table("event_subgroups") {
    val eventId = uuid("event_id").references(EventsTable.id, onDelete = ReferenceOption.CASCADE)
    val subgroupId = uuid("subgroup_id").references(SubgroupsTable.id, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(eventId, subgroupId)
}
