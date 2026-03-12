package com.playbook.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object EventsTable : Table("events") {
    val id = uuid("id").autoGenerate()
    val title = text("title")
    val type = text("type")
    val startAt = timestampWithTimeZone("start_at")
    val endAt = timestampWithTimeZone("end_at")
    val meetupAt = timestampWithTimeZone("meetup_at").nullable()
    val location = text("location").nullable()
    val description = text("description").nullable()
    val minAttendees = integer("min_attendees").nullable()
    val status = text("status").default("active")
    val cancelledAt = timestampWithTimeZone("cancelled_at").nullable()
    val seriesId = uuid("series_id").references(EventSeriesTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val seriesSequence = integer("series_sequence").nullable()
    val seriesOverride = bool("series_override").default(false)
    val createdBy = uuid("created_by").references(UsersTable.id, onDelete = ReferenceOption.RESTRICT)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}
