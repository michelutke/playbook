package com.playbook.data.repository

import com.playbook.data.network.ApiConfig
import com.playbook.domain.CancelEventRequest
import com.playbook.domain.CreateEventRequest
import com.playbook.domain.Event
import com.playbook.domain.EventType
import com.playbook.domain.UpdateEventRequest
import com.playbook.repository.EventRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.datetime.Instant

class EventRepositoryImpl(
    private val client: HttpClient,
    private val config: ApiConfig,
) : EventRepository {

    override suspend fun listForUser(
        userId: String,
        from: Instant?,
        to: Instant?,
        type: EventType?,
        teamId: String?,
    ): List<Event> = client.get("${config.baseUrl}/users/me/events") {
        bearerAuth(config.authTokenProvider() ?: "")
        from?.let { parameter("from", it.toString()) }
        to?.let { parameter("to", it.toString()) }
        type?.let { parameter("type", it.name.lowercase()) }
        teamId?.let { parameter("teamId", it) }
    }.body()

    override suspend fun listForTeam(
        teamId: String,
        from: Instant?,
        to: Instant?,
        type: EventType?,
    ): List<Event> = client.get("${config.baseUrl}/teams/$teamId/events") {
        bearerAuth(config.authTokenProvider() ?: "")
        from?.let { parameter("from", it.toString()) }
        to?.let { parameter("to", it.toString()) }
        type?.let { parameter("type", it.name.lowercase()) }
    }.body()

    override suspend fun getById(eventId: String): Event? =
        client.get("${config.baseUrl}/events/$eventId") {
            bearerAuth(config.authTokenProvider() ?: "")
        }.body()

    override suspend fun create(request: CreateEventRequest, createdBy: String): Event =
        client.post("${config.baseUrl}/events") {
            bearerAuth(config.authTokenProvider() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun update(eventId: String, request: UpdateEventRequest): Event =
        client.patch("${config.baseUrl}/events/$eventId") {
            bearerAuth(config.authTokenProvider() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun cancel(eventId: String, request: CancelEventRequest): Event =
        client.post("${config.baseUrl}/events/$eventId/cancel") {
            bearerAuth(config.authTokenProvider() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun duplicate(eventId: String): CreateEventRequest =
        client.post("${config.baseUrl}/events/$eventId/duplicate") {
            bearerAuth(config.authTokenProvider() ?: "")
        }.body()

    override suspend fun resolveTargetedUsers(eventId: String): List<String> =
        throw UnsupportedOperationException("resolveTargetedUsers is a backend-only operation")
}
