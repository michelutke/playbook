package ch.teamorg.routes

import ch.teamorg.domain.models.CreateEventRequest
import ch.teamorg.domain.models.EditEventRequest
import ch.teamorg.domain.models.RecurringScope
import ch.teamorg.domain.repositories.EventRepository
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import java.time.Instant
import java.util.UUID

@Serializable
private data class EditEventWithScope(
    val scope: String? = "this_only",
    val title: String? = null,
    val type: String? = null,
    val startAt: String? = null,
    val endAt: String? = null,
    val meetupAt: String? = null,
    val location: String? = null,
    val description: String? = null,
    val minAttendees: Int? = null,
    val teamIds: List<String>? = null,
    val subgroupIds: List<String>? = null
)

@Serializable
private data class CancelScopeRequest(val scope: String? = "this_only")

fun Route.eventRoutes() {
    val eventRepository by inject<EventRepository>()

    authenticate("jwt") {
        get("/users/me/events") {
            val userId = UUID.fromString(call.principal<JWTPrincipal>()!!.payload.subject)
            val from = call.parameters["from"]?.let { Instant.parse(it) }
            val to = call.parameters["to"]?.let { Instant.parse(it) }
            val type = call.parameters["type"]
            val teamId = call.parameters["teamId"]?.let { UUID.fromString(it) }
            val events = eventRepository.findEventsForUser(userId, from, to, type, teamId)
            call.respond(events)
        }

        get("/teams/{teamId}/events") {
            val teamId = UUID.fromString(call.parameters["teamId"])
            val from = call.parameters["from"]?.let { Instant.parse(it) }
            val to = call.parameters["to"]?.let { Instant.parse(it) }
            val events = eventRepository.findEventsForTeam(teamId, from, to)
            call.respond(events)
        }

        get("/events/{id}") {
            val id = UUID.fromString(call.parameters["id"])
            val event = eventRepository.findByIdWithTeams(id)
            if (event != null) call.respond(event)
            else call.respond(HttpStatusCode.NotFound)
        }

        post("/events") {
            val userId = UUID.fromString(call.principal<JWTPrincipal>()!!.payload.subject)
            val request = call.receive<CreateEventRequest>()
            if (request.recurring != null) {
                val series = eventRepository.createSeries(request, userId)
                eventRepository.materialiseUpcomingOccurrences()
                val events = eventRepository.findEventsForUser(
                    userId, from = request.startAt, to = null, type = null, teamId = null
                )
                call.respond(HttpStatusCode.Created, events.firstOrNull() ?: mapOf("seriesId" to series.id.toString()))
            } else {
                val event = eventRepository.create(request, userId)
                call.respond(HttpStatusCode.Created, event)
            }
        }

        patch("/events/{id}") {
            val id = UUID.fromString(call.parameters["id"])
            val body = call.receive<EditEventWithScope>()
            val scope = RecurringScope.valueOf(body.scope ?: "this_only")
            val editRequest = EditEventRequest(
                title = body.title,
                type = body.type,
                startAt = body.startAt?.let { Instant.parse(it) },
                endAt = body.endAt?.let { Instant.parse(it) },
                meetupAt = body.meetupAt?.let { Instant.parse(it) },
                location = body.location,
                description = body.description,
                minAttendees = body.minAttendees,
                teamIds = body.teamIds?.map { UUID.fromString(it) },
                subgroupIds = body.subgroupIds?.map { UUID.fromString(it) }
            )

            val existing = eventRepository.findById(id)
            if (existing == null) {
                call.respond(HttpStatusCode.NotFound)
                return@patch
            }

            when (scope) {
                RecurringScope.this_only -> {
                    eventRepository.update(id, editRequest)
                }
                RecurringScope.this_and_future -> {
                    if (existing.seriesId != null && existing.seriesSequence != null) {
                        eventRepository.cancelFutureInSeries(existing.seriesId, existing.seriesSequence)
                        val newRequest = CreateEventRequest(
                            title = editRequest.title ?: existing.title,
                            type = editRequest.type ?: existing.type,
                            startAt = editRequest.startAt ?: existing.startAt,
                            endAt = editRequest.endAt ?: existing.endAt,
                            meetupAt = editRequest.meetupAt ?: existing.meetupAt,
                            location = editRequest.location ?: existing.location,
                            description = editRequest.description ?: existing.description,
                            minAttendees = editRequest.minAttendees ?: existing.minAttendees,
                            teamIds = editRequest.teamIds ?: existing.teamIds,
                            subgroupIds = editRequest.subgroupIds ?: existing.subgroupIds,
                            recurring = null
                        )
                        val userId = UUID.fromString(call.principal<JWTPrincipal>()!!.payload.subject)
                        eventRepository.create(newRequest, userId)
                    } else {
                        eventRepository.update(id, editRequest)
                    }
                }
                RecurringScope.all -> {
                    if (existing.seriesId != null) {
                        eventRepository.updateSeriesTemplate(existing.seriesId, editRequest)
                        eventRepository.updateFutureInSeries(
                            existing.seriesId, existing.seriesSequence ?: 0, editRequest
                        )
                    } else {
                        eventRepository.update(id, editRequest)
                    }
                }
            }

            val updated = eventRepository.findByIdWithTeams(id)
            if (updated != null) call.respond(updated)
            else call.respond(HttpStatusCode.NotFound)
        }

        post("/events/{id}/cancel") {
            val id = UUID.fromString(call.parameters["id"])
            val body = call.receive<CancelScopeRequest>()
            val scope = RecurringScope.valueOf(body.scope ?: "this_only")

            val existing = eventRepository.findById(id)
            if (existing == null) {
                call.respond(HttpStatusCode.NotFound)
                return@post
            }

            when (scope) {
                RecurringScope.this_only -> eventRepository.cancel(id)
                RecurringScope.this_and_future -> {
                    if (existing.seriesId != null && existing.seriesSequence != null) {
                        eventRepository.cancel(id)
                        eventRepository.cancelFutureInSeries(existing.seriesId, existing.seriesSequence + 1)
                    } else {
                        eventRepository.cancel(id)
                    }
                }
                RecurringScope.all -> {
                    if (existing.seriesId != null) {
                        eventRepository.cancelFutureInSeries(existing.seriesId, 0)
                    }
                    eventRepository.cancel(id)
                }
            }

            call.respond(HttpStatusCode.OK)
        }

        post("/events/{id}/duplicate") {
            val userId = UUID.fromString(call.principal<JWTPrincipal>()!!.payload.subject)
            val id = UUID.fromString(call.parameters["id"])
            val event = eventRepository.duplicate(id, userId)
            if (event != null) call.respond(HttpStatusCode.Created, event)
            else call.respond(HttpStatusCode.NotFound)
        }
    }
}
