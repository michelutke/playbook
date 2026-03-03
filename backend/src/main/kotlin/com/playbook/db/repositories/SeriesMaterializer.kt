package com.playbook.db.repositories

import com.playbook.db.tables.EventSeriesTable
import com.playbook.db.tables.EventSubgroupsTable
import com.playbook.db.tables.EventTeamsTable
import com.playbook.db.tables.EventsTable
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaInstant
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.ZoneOffset
import java.util.UUID

/**
 * Event content fields copied to each materialized occurrence.
 */
data class EventContent(
    val title: String,
    val type: String,
    val location: String? = null,
    val description: String? = null,
    val minAttendees: Int? = null,
)

/**
 * ES-021: Idempotent series occurrence generator.
 * Upserts event rows for a series within [fromDate, toDate].
 * Non-override occurrences that already exist (by date) are skipped.
 */
suspend fun materializeSeries(
    seriesId: UUID,
    fromDate: LocalDate,
    toDate: LocalDate,
    teamIds: List<UUID>,
    subgroupIds: List<UUID>,
    content: EventContent,
) = newSuspendedTransaction {
    val series = EventSeriesTable.selectAll().where { EventSeriesTable.id eq seriesId }.singleOrNull()
        ?: return@newSuspendedTransaction

    val seriesStart = series[EventSeriesTable.seriesStartDate]
    val seriesEnd = series[EventSeriesTable.seriesEndDate]

    val effectiveFrom = maxOf(seriesStart, fromDate)
    val effectiveTo = seriesEnd?.let { minOf(it, toDate) } ?: toDate

    if (effectiveFrom > effectiveTo) return@newSuspendedTransaction

    val startTime = series[EventSeriesTable.templateStartTime]
    val endTime = series[EventSeriesTable.templateEndTime]
    val patternType = series[EventSeriesTable.patternType]
    val weekdays = series[EventSeriesTable.weekdays]
        ?.split(",")?.mapNotNull { it.trim().toIntOrNull() } ?: emptyList()
    val intervalDays = series[EventSeriesTable.intervalDays] ?: 1

    // Existing non-override occurrence dates — skip these to stay idempotent
    val existingDates = EventsTable
        .selectAll().where { (EventsTable.seriesId eq seriesId) and (EventsTable.seriesOverride eq false) }
        .map { it[EventsTable.startAt].toLocalDate().toKotlinLocalDate() }
        .toSet()

    val maxSeq = EventsTable
        .selectAll().where { EventsTable.seriesId eq seriesId }
        .mapNotNull { it[EventsTable.seriesSequence] }
        .maxOrNull() ?: 0

    val occurrences = generateOccurrenceDates(patternType, weekdays, intervalDays, effectiveFrom, effectiveTo)
    var nextSeq = maxSeq

    occurrences.forEach { date ->
        if (date in existingDates) return@forEach
        nextSeq++

        val startInstant = LocalDateTime(date, startTime).toInstant(TimeZone.UTC)
        val endInstant = LocalDateTime(date, endTime).toInstant(TimeZone.UTC)
        val startODT = startInstant.toJavaInstant().atOffset(ZoneOffset.UTC)
        val endODT = endInstant.toJavaInstant().atOffset(ZoneOffset.UTC)
        val now = java.time.OffsetDateTime.now(ZoneOffset.UTC)

        val eventId = UUID.randomUUID()
        EventsTable.insert {
            it[id] = eventId
            it[title] = content.title
            it[type] = content.type
            it[EventsTable.startAt] = startODT
            it[EventsTable.endAt] = endODT
            it[location] = content.location
            it[description] = content.description
            it[minAttendees] = content.minAttendees
            it[status] = "active"
            it[EventsTable.seriesId] = seriesId
            it[seriesSequence] = nextSeq
            it[seriesOverride] = false
            it[createdBy] = series[EventSeriesTable.createdBy]
            it[createdAt] = now
            it[updatedAt] = now
        }

        teamIds.forEach { tid ->
            EventTeamsTable.insertIgnore {
                it[EventTeamsTable.eventId] = eventId
                it[EventTeamsTable.teamId] = tid
            }
        }
        subgroupIds.forEach { sid ->
            EventSubgroupsTable.insertIgnore {
                it[EventSubgroupsTable.eventId] = eventId
                it[EventSubgroupsTable.subgroupId] = sid
            }
        }
    }
}

internal fun generateOccurrenceDates(
    patternType: String,
    weekdays: List<Int>,
    intervalDays: Int,
    from: LocalDate,
    to: LocalDate,
): List<LocalDate> {
    val dates = mutableListOf<LocalDate>()
    var current = from
    while (current <= to) {
        when (patternType) {
            "daily" -> {
                dates.add(current)
                current = current.plus(1, DateTimeUnit.DAY)
            }
            "weekly" -> {
                // dayOfWeek.ordinal: 0=Mon … 6=Sun (matches spec weekday encoding)
                if (current.dayOfWeek.ordinal in weekdays) dates.add(current)
                current = current.plus(1, DateTimeUnit.DAY)
            }
            "custom" -> {
                dates.add(current)
                current = current.plus(maxOf(1, intervalDays), DateTimeUnit.DAY)
            }
            else -> break
        }
    }
    return dates
}

internal fun java.time.LocalDate.toKotlinLocalDate(): LocalDate =
    LocalDate(year, monthValue, dayOfMonth)
