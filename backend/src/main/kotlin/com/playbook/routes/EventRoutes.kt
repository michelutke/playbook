package com.playbook.routes

import com.playbook.domain.CancelEventRequest
import com.playbook.domain.CreateEventRequest
import com.playbook.domain.EventType
import com.playbook.domain.UpdateEventRequest
import com.playbook.middleware.requireCoachOnEventTeam
import com.playbook.middleware.requireCoachOnTeam
import com.playbook.plugins.NotFoundException
import com.playbook.plugins.userId
import com.playbook.repository.EventRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Instant
import org.koin.ktor.ext.inject

fun Route.registerEventRoutes() {
    val eventRepo: EventRepository by inject()

    // ES-009: GET /users/me/events — deduplicated player view
    get("/users/me/events") {
        val uid = call.userId
        val params = call.request.queryParameters
        val from = params["from"]?.let { Instant.parse(it) }
        val to = params["to"]?.let { Instant.parse(it) }
        val type = params["type"]?.let { runCatching { EventType.valueOf(it.uppercase()) }.getOrNull() }
        val teamId = params["teamId"]
        val events = eventRepo.listForUser(uid, from, to, type, teamId)
        call.respond(events)
    }

    // ES-010: GET /teams/{teamId}/events — coach view
    get("/teams/{teamId}/events") {
        val teamId = call.parameters["teamId"]!!
        requireCoachOnTeam(teamId)
        val params = call.request.queryParameters
        val from = params["from"]?.let { Instant.parse(it) }
        val to = params["to"]?.let { Instant.parse(it) }
        val type = params["type"]?.let { runCatching { EventType.valueOf(it.uppercase()) }.getOrNull() }
        val events = eventRepo.listForTeam(teamId, from, to, type)
        call.respond(events)
    }

    // ES-011: GET /events/{id}
    get("/events/{id}") {
        val id = call.parameters["id"]!!
        val event = eventRepo.getById(id) ?: throw NotFoundException("Event not found")
        call.respond(event)
    }

    // ES-012: POST /events
    post("/events") {
        val uid = call.userId
        val request = call.receive<CreateEventRequest>()
        // Verify coach on at least one target team
        for (teamId in request.teamIds) {
            runCatching { requireCoachOnTeam(teamId) }.onSuccess { break }
        }
        val event = eventRepo.create(request, uid)
        call.respond(HttpStatusCode.Created, event)
    }

    // ES-013: PATCH /events/{id}
    patch("/events/{id}") {
        val id = call.parameters["id"]!!
        requireCoachOnEventTeam(id)
        val request = call.receive<UpdateEventRequest>()
        val event = eventRepo.update(id, request)
        call.respond(event)
    }

    // ES-014: POST /events/{id}/cancel
    post("/events/{id}/cancel") {
        val id = call.parameters["id"]!!
        requireCoachOnEventTeam(id)
        val request = call.receiveNullable<CancelEventRequest>() ?: CancelEventRequest()
        val event = eventRepo.cancel(id, request)
        call.respond(event)
    }

    // ES-015: POST /events/{id}/duplicate
    post("/events/{id}/duplicate") {
        val id = call.parameters["id"]!!
        requireCoachOnEventTeam(id)
        val payload = eventRepo.duplicate(id)
        call.respond(payload)
    }
}
