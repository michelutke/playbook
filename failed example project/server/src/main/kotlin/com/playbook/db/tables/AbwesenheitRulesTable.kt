package com.playbook.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object AbwesenheitRulesTable : Table("abwesenheit_rules") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val presetType = text("preset_type")
    val label = text("label")
    val ruleType = text("rule_type")
    // stored as comma-separated string e.g. "0,1,2" (Mon=0); null means all or N/A
    val weekdays = text("weekdays").nullable()
    val startDate = date("start_date").nullable()
    val endDate = date("end_date").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}
