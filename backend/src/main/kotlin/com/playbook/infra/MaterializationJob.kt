package com.playbook.infra

import com.playbook.db.repositories.EventContent
import com.playbook.db.repositories.materializeSeries
import com.playbook.db.tables.EventSeriesTable
import com.playbook.db.tables.EventSubgroupsTable
import com.playbook.db.tables.EventTeamsTable
import com.playbook.db.tables.EventsTable
import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * ES-023: Daily coroutine ensuring every active series has occurrences ≥ 12 months ahead.
 */
fun Application.startMaterializationJob() {
    val scope = CoroutineScope(Dispatchers.IO)
    scope.launch {
        while (isActive) {
            runCatching { rollForwardAllSeries() }
                .onFailure { log.error("Materialisation job failed", it) }
            delay(TimeUnit.HOURS.toMillis(24))
        }
    }
}

private suspend fun rollForwardAllSeries() {
    val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
    val horizon = today.plus(DatePeriod(months = 12))

    // Fetch all series IDs where seriesEndDate is null or still in future (filter in Kotlin to avoid nullable column comparison)
    val seriesIds = newSuspendedTransaction {
        EventSeriesTable.selectAll().toList()
            .filter { row ->
                val endDate: LocalDate? = row[EventSeriesTable.seriesEndDate]
                endDate == null || endDate >= today
            }
            .map { it[EventSeriesTable.id] }
    }

    seriesIds.forEach { seriesId ->
        runCatching {
            val ctx = resolveSeriesContext(seriesId) ?: return@forEach
            materializeSeries(
                seriesId = seriesId,
                fromDate = today,
                toDate = horizon,
                teamIds = ctx.first,
                subgroupIds = ctx.second,
                content = ctx.third,
            )
        }.onFailure { /* skip failed series; retry next day */ }
    }
}

private suspend fun resolveSeriesContext(
    seriesId: UUID,
): Triple<List<UUID>, List<UUID>, EventContent>? = newSuspendedTransaction {
    val lastEvent = EventsTable
        .select { (EventsTable.seriesId eq seriesId) and (EventsTable.seriesOverride eq false) }
        .sortedByDescending { it[EventsTable.startAt] }
        .firstOrNull() ?: return@newSuspendedTransaction null

    val eventId = lastEvent[EventsTable.id]
    val teamIds = EventTeamsTable.select { EventTeamsTable.eventId eq eventId }.map { it[EventTeamsTable.teamId] }
    val subgroupIds = EventSubgroupsTable.select { EventSubgroupsTable.eventId eq eventId }.map { it[EventSubgroupsTable.subgroupId] }

    Triple(
        teamIds,
        subgroupIds,
        EventContent(
            title = lastEvent[EventsTable.title],
            type = lastEvent[EventsTable.type],
            location = lastEvent[EventsTable.location],
            description = lastEvent[EventsTable.description],
            minAttendees = lastEvent[EventsTable.minAttendees],
        ),
    )
}
