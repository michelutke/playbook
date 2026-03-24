package ch.teamorg.repository

import ch.teamorg.domain.CreateEventRequest
import ch.teamorg.domain.EditEventRequest
import ch.teamorg.domain.Event
import ch.teamorg.domain.EventWithTeams
import ch.teamorg.domain.SubGroup

interface EventRepository {
    suspend fun getMyEvents(
        from: String? = null,
        to: String? = null,
        type: String? = null,
        teamId: String? = null
    ): Result<List<EventWithTeams>>

    suspend fun getEventDetail(id: String): Result<EventWithTeams>

    suspend fun createEvent(request: CreateEventRequest): Result<Event>

    suspend fun editEvent(id: String, request: EditEventRequest): Result<Event>

    suspend fun cancelEvent(id: String, scope: String = "this_only"): Result<Unit>

    suspend fun uncancelEvent(id: String, scope: String = "this_only"): Result<Unit>

    suspend fun duplicateEvent(id: String): Result<Event>

    suspend fun getSubGroups(teamId: String): Result<List<SubGroup>>
}
