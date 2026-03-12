package com.playbook.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.time
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object EventSeriesTable : Table("event_series") {
    val id = uuid("id").autoGenerate()
    val patternType = text("pattern_type")
    val weekdays = text("weekdays").nullable()       // "0,2,4" = Mon/Wed/Fri; null if not weekly
    val intervalDays = integer("interval_days").nullable()
    val seriesStartDate = date("series_start_date")
    val seriesEndDate = date("series_end_date").nullable()
    val templateStartTime = time("template_start_time")
    val templateEndTime = time("template_end_time")
    val createdBy = uuid("created_by").references(UsersTable.id, onDelete = ReferenceOption.RESTRICT)
    val createdAt = timestampWithTimeZone("created_at")
    override val primaryKey = PrimaryKey(id)
}
