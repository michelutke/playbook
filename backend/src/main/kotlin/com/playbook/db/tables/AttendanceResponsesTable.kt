package com.playbook.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object AttendanceResponsesTable : Table("attendance_responses") {
    val eventId = uuid("event_id").references(EventsTable.id, onDelete = ReferenceOption.CASCADE)
    val userId = uuid("user_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val status = text("status").default("no-response")
    val reason = text("reason").nullable()
    val abwesenheitRuleId = uuid("abwesenheit_rule_id").references(AbwesenheitRulesTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val manualOverride = bool("manual_override").default(false)
    val respondedAt = timestampWithTimeZone("responded_at").nullable()
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(eventId, userId)
}
