package ch.teamorg.routes

import ch.teamorg.domain.repositories.AttendanceRepository
import ch.teamorg.domain.repositories.AttendanceResponseDto
import ch.teamorg.domain.repositories.AttendanceResponseRow
import ch.teamorg.domain.repositories.RawAttendanceRow
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
import kotlinx.datetime.Instant as KInstant

@Serializable
private data class SubmitResponseRequest(val status: String, val reason: String? = null)

@Serializable
private data class RawAttendanceDto(
    val eventId: String,
    val userId: String,
    val responseStatus: String?,
    val recordStatus: String?,
    val eventStartAt: KInstant
)

private fun AttendanceResponseRow.toDto() = AttendanceResponseDto(
    eventId = eventId.toString(),
    userId = userId.toString(),
    status = status,
    reason = reason,
    abwesenheitRuleId = abwesenheitRuleId?.toString(),
    manualOverride = manualOverride,
    respondedAt = respondedAt?.let { KInstant.fromEpochMilliseconds(it.toEpochMilli()) },
    updatedAt = KInstant.fromEpochMilliseconds(updatedAt.toEpochMilli())
)

private fun RawAttendanceRow.toDto() = RawAttendanceDto(
    eventId = eventId.toString(),
    userId = userId.toString(),
    responseStatus = responseStatus,
    recordStatus = recordStatus,
    eventStartAt = KInstant.fromEpochMilliseconds(eventStartAt.toEpochMilli())
)

fun Route.attendanceRoutes() {
    val attendanceRepo by inject<AttendanceRepository>()

    authenticate("jwt") {
        get("/events/{id}/attendance") {
            val eventId = UUID.fromString(call.parameters["id"])
            val responses = attendanceRepo.getEventAttendance(eventId)
            call.respond(responses.map { it.toDto() })
        }

        get("/events/{id}/attendance/me") {
            val eventId = UUID.fromString(call.parameters["id"])
            val userId = UUID.fromString(call.principal<JWTPrincipal>()!!.payload.subject)
            val response = attendanceRepo.getMyResponse(eventId, userId)
            if (response != null) call.respond(response.toDto())
            else call.respond(HttpStatusCode.NoContent)
        }

        put("/events/{id}/attendance/me") {
            val eventId = UUID.fromString(call.parameters["id"])
            val userId = UUID.fromString(call.principal<JWTPrincipal>()!!.payload.subject)
            val body = call.receive<SubmitResponseRequest>()

            if (body.status == "unsure" && body.reason.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Reason required for unsure status")
                return@put
            }

            if (attendanceRepo.isDeadlinePassed(eventId)) {
                call.respond(HttpStatusCode.Conflict, "Response deadline has passed")
                return@put
            }

            val updated = attendanceRepo.upsertResponse(eventId, userId, body.status, body.reason)
            call.respond(updated.toDto())
        }

        get("/users/{userId}/attendance") {
            val userId = UUID.fromString(call.parameters["userId"])
            val from = call.parameters["from"]?.let { Instant.parse(it) }
            val to = call.parameters["to"]?.let { Instant.parse(it) }
            val rows = attendanceRepo.getRawAttendance(userId, from, to)
            call.respond(rows.map { it.toDto() })
        }

        get("/teams/{teamId}/attendance") {
            val teamId = UUID.fromString(call.parameters["teamId"])
            val from = call.parameters["from"]?.let { Instant.parse(it) }
            val to = call.parameters["to"]?.let { Instant.parse(it) }
            val rows = attendanceRepo.getTeamAttendance(teamId, from, to)
            call.respond(rows.map { it.toDto() })
        }
    }
}
