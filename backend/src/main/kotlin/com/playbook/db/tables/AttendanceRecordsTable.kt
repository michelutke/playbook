package com.playbook.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object AttendanceRecordsTable : Table("attendance_records") {
    val eventId = uuid("event_id").references(EventsTable.id, onDelete = ReferenceOption.CASCADE)
    val userId = uuid("user_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val status = text("status")
    val note = text("note").nullable()
    val setBy = uuid("set_by").references(UsersTable.id, onDelete = ReferenceOption.RESTRICT)
    val setAt = timestampWithTimeZone("set_at")
    val previousStatus = text("previous_status").nullable()
    val previousSetBy = uuid("previous_set_by").references(UsersTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    override val primaryKey = PrimaryKey(eventId, userId)
}
