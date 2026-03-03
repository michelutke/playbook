package com.playbook.routes

import com.playbook.domain.UpdateCheckInRequest
import com.playbook.middleware.requireCoachOnEventTeam
import com.playbook.repository.AttendanceRepository
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.registerCheckInRoutes() {
    val attendanceRepo: AttendanceRepository by inject()

    // T-013: GET /events/{id}/check-in (coach only)
    get("/events/{id}/check-in") {
        val eventId = call.parameters["id"]!!
        requireCoachOnEventTeam(eventId)
        val list = attendanceRepo.getCheckInList(eventId)
        call.respond(list)
    }

    // T-014: PUT /events/{id}/check-in/{userId}
    put("/events/{id}/check-in/{userId}") {
        val eventId = call.parameters["id"]!!
        val targetUserId = call.parameters["userId"]!!
        requireCoachOnEventTeam(eventId)
        val request = call.receive<UpdateCheckInRequest>()
        val record = attendanceRepo.setCheckIn(eventId, targetUserId, request)
        call.respond(record)
    }
}
