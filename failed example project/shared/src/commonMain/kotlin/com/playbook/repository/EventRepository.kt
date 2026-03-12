package com.playbook.repository

import com.playbook.domain.CancelEventRequest
import com.playbook.domain.CreateEventRequest
import com.playbook.domain.Event
import com.playbook.domain.EventType
import com.playbook.domain.UpdateEventRequest
import kotlinx.datetime.Instant

interface EventRepository {
    suspend fun listForUser(
        userId: String,
        from: Instant? = null,
        to: Instant? = null,
        type: EventType? = null,
        teamId: String? = null,
    ): List<Event>

    suspend fun listForTeam(
        teamId: String,
        from: Instant? = null,
        to: Instant? = null,
        type: EventType? = null,
    ): List<Event>

    suspend fun getById(eventId: String): Event?
    suspend fun create(request: CreateEventRequest, createdBy: String): Event
    suspend fun update(eventId: String, request: UpdateEventRequest): Event
    suspend fun cancel(eventId: String, request: CancelEventRequest): Event
    suspend fun duplicate(eventId: String): CreateEventRequest
    suspend fun resolveTargetedUsers(eventId: String): List<String>
}
