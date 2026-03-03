package com.playbook.routes

import com.playbook.domain.UpdateAttendanceRequest
import com.playbook.middleware.requireCoachOnTeam
import com.playbook.middleware.requireMemberOnEventTeam
import com.playbook.middleware.requireSelfOrSharedCoach
import com.playbook.plugins.userId
import com.playbook.repository.AttendanceRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Instant
import org.koin.ktor.ext.inject

fun Route.registerAttendanceRoutes() {
    val attendanceRepo: AttendanceRepository by inject()

    // T-005: GET /events/{id}/attendance - team view
    get("/events/{id}/attendance") {
        val eventId = call.parameters["id"]!!
        requireMemberOnEventTeam(eventId)
        val view = attendanceRepo.getEventAttendance(eventId)
        call.respond(view)
    }

    // T-006: GET /events/{id}/attendance/me
    get("/events/{id}/attendance/me") {
        val eventId = call.parameters["id"]!!
        val uid = call.userId
        val response = attendanceRepo.getMyAttendance(eventId, uid)
        call.respond(response)
    }

    // T-007: PUT /events/{id}/attendance/me
    put("/events/{id}/attendance/me") {
        val eventId = call.parameters["id"]!!
        val uid = call.userId
        val request = call.receive<UpdateAttendanceRequest>()
        val response = attendanceRepo.upsertAttendance(eventId, uid, request)
        call.respond(response)
    }

    // T-015: GET /users/{userId}/attendance
    get("/users/{userId}/attendance") {
        val targetUserId = call.parameters["userId"]!!
        requireSelfOrSharedCoach(targetUserId)
        val params = call.request.queryParameters
        val from = params["from"]?.let { Instant.parse(it) }
        val to = params["to"]?.let { Instant.parse(it) }
        val rows = attendanceRepo.getUserAttendance(targetUserId, from, to)
        call.respond(rows)
    }

    // T-016: GET /teams/{teamId}/attendance
    get("/teams/{teamId}/attendance") {
        val teamId = call.parameters["teamId"]!!
        requireCoachOnTeam(teamId)
        val params = call.request.queryParameters
        val from = params["from"]?.let { Instant.parse(it) }
        val to = params["to"]?.let { Instant.parse(it) }
        val rows = attendanceRepo.getTeamAttendance(teamId, from, to)
        call.respond(rows)
    }
}
