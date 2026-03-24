package ch.teamorg.routes

import ch.teamorg.domain.repositories.AttendanceRepository
import ch.teamorg.domain.repositories.TeamRepository
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import java.util.UUID

@Serializable
private data class CheckInRequest(val status: String, val note: String? = null)

fun Route.checkInRoutes() {
    val attendanceRepo by inject<AttendanceRepository>()
    val teamRepository by inject<TeamRepository>()

    authenticate("jwt") {
        get("/events/{id}/check-in") {
            val eventId = UUID.fromString(call.parameters["id"])
            val entries = attendanceRepo.getCheckInEntries(eventId)
            call.respond(entries)
        }

        put("/events/{id}/check-in/{userId}") {
            val eventId = UUID.fromString(call.parameters["id"])
            val targetUserId = UUID.fromString(call.parameters["userId"])
            val coachId = UUID.fromString(call.principal<JWTPrincipal>()!!.payload.subject)
            val body = call.receive<CheckInRequest>()

            // Require coach or club_manager role
            val teamRoles = teamRepository.getUserTeamRoles(coachId)
            val clubRoles = teamRepository.getUserClubRoles(coachId)
            val isCoachOrManager = teamRoles.any { it.third == "coach" } ||
                clubRoles.any { it.second == "club_manager" }
            if (!isCoachOrManager) {
                call.respond(HttpStatusCode.Forbidden, "Coach role required")
                return@put
            }

            val record = attendanceRepo.upsertCheckIn(
                eventId = eventId,
                userId = targetUserId,
                status = body.status,
                note = body.note,
                setBy = coachId
            )
            call.respond(record)
        }
    }
}
