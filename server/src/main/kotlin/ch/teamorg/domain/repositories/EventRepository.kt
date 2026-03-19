package ch.teamorg.domain.repositories

import ch.teamorg.domain.models.*
import java.time.Instant
import java.util.UUID

interface EventRepository {
    suspend fun create(request: CreateEventRequest, createdBy: UUID): Event
    suspend fun findById(id: UUID): Event?
    suspend fun findByIdWithTeams(id: UUID): EventWithTeams?
    suspend fun findEventsForUser(
        userId: UUID,
        from: Instant? = null,
        to: Instant? = null,
        type: String? = null,
        teamId: UUID? = null
    ): List<EventWithTeams>
    suspend fun findEventsForTeam(teamId: UUID, from: Instant?, to: Instant?): List<Event>
    suspend fun update(id: UUID, request: EditEventRequest): Event?
    suspend fun cancel(id: UUID): Event?
    suspend fun duplicate(id: UUID, createdBy: UUID): Event?
    suspend fun createSeries(request: CreateEventRequest, createdBy: UUID): EventSeries
    suspend fun findSeriesById(id: UUID): EventSeries?
    suspend fun updateSeriesTemplate(seriesId: UUID, request: EditEventRequest)
    suspend fun materialiseUpcomingOccurrences(): Int
    suspend fun cancelFutureInSeries(seriesId: UUID, fromSequence: Int): Int
    suspend fun updateFutureInSeries(seriesId: UUID, fromSequence: Int, request: EditEventRequest): Int
}
