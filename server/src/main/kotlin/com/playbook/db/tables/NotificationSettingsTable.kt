package com.playbook.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object NotificationSettingsTable : Table("notification_settings") {
    val userId = uuid("user_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val newEvents = bool("new_events").default(true)
    val eventChanges = bool("event_changes").default(true)
    val eventCancellations = bool("event_cancellations").default(true)
    val reminders = bool("reminders").default(true)
    val reminderLeadTime = text("reminder_lead_time").default("1d")
    val attendancePerResponse = bool("attendance_per_response").default(true)
    val attendanceSummary = bool("attendance_summary").default(false)
    val attendanceSummaryLeadTime = text("attendance_summary_lead_time").default("2h")
    val abwesenheitChanges = bool("abwesenheit_changes").default(true)
    override val primaryKey = PrimaryKey(userId)
}
