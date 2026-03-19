package ch.teamorg.data.repository

import ch.teamorg.data.EventCacheManager
import ch.teamorg.domain.CreateEventRequest
import ch.teamorg.domain.EditEventRequest
import ch.teamorg.domain.Event
import ch.teamorg.domain.EventWithTeams
import ch.teamorg.domain.SubGroup
import ch.teamorg.repository.EventRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.errors.IOException
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus

class EventRepositoryImpl(
    private val httpClient: HttpClient,
    private val cacheManager: EventCacheManager
) : EventRepository {

    override suspend fun getMyEvents(
        from: String?,
        to: String?,
        type: String?,
        teamId: String?
    ): Result<List<EventWithTeams>> {
        return try {
            val events: List<EventWithTeams> = httpClient.get("/users/me/events") {
                from?.let { parameter("from", it) }
                to?.let { parameter("to", it) }
                type?.let { parameter("type", it) }
                teamId?.let { parameter("teamId", it) }
            }.body()
            cacheManager.saveEvents(events)
            Result.success(events)
        } catch (e: ConnectTimeoutException) {
            offlineFallback(type, teamId)
        } catch (e: HttpRequestTimeoutException) {
            offlineFallback(type, teamId)
        } catch (e: IOException) {
            offlineFallback(type, teamId)
        }
        // ResponseException (4xx/5xx) propagates to the caller — not caught here
    }

    private fun offlineFallback(type: String?, teamId: String?): Result<List<EventWithTeams>> {
        val now = Clock.System.now().toEpochMilliseconds()
        val threeMonths = Clock.System.now()
            .plus(90, DateTimeUnit.DAY, TimeZone.UTC)
            .toEpochMilliseconds()
        return Result.success(cacheManager.getFilteredOfflineEvents(now, threeMonths, type, teamId))
    }

    override suspend fun getEventDetail(id: String): Result<EventWithTeams> = runCatching {
        httpClient.get("/events/$id").body()
    }

    override suspend fun createEvent(request: CreateEventRequest): Result<Event> = runCatching {
        httpClient.post("/events") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun editEvent(id: String, request: EditEventRequest): Result<Event> = runCatching {
        httpClient.patch("/events/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun cancelEvent(id: String, scope: String): Result<Unit> = runCatching {
        httpClient.post("/events/$id/cancel") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("scope" to scope))
        }
        Unit
    }

    override suspend fun duplicateEvent(id: String): Result<Event> = runCatching {
        httpClient.post("/events/$id/duplicate").body()
    }

    override suspend fun getSubGroups(teamId: String): Result<List<SubGroup>> = runCatching {
        httpClient.get("/teams/$teamId/subgroups").body()
    }
}
