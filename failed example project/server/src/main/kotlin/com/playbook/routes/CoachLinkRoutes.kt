package com.playbook.routes

import com.playbook.domain.CreateCoachLinkRequest
import com.playbook.middleware.requireClubManager
import com.playbook.plugins.NotFoundException
import com.playbook.plugins.userId
import com.playbook.plugins.userIdOrNull
import com.playbook.repository.CoachLinkRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.registerCoachLinkRoutes(authenticated: Boolean) {
    val coachLinkRepo: CoachLinkRepository by inject()

    if (!authenticated) {
        // TM-040: GET /club-links/{token} — public
        get("/club-links/{token}") {
            val token = call.parameters["token"]!!
            val context = coachLinkRepo.resolveToken(token)
            if (context == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Link not found"))
                return@get
            }
            call.respond(context)
        }

        // TM-041: POST /club-links/{token}/join
        post("/club-links/{token}/join") {
            val token = call.parameters["token"]!!
            val uid = call.userIdOrNull ?: throw IllegalArgumentException("Authentication required to join")
            coachLinkRepo.join(token, uid)
            call.respond(HttpStatusCode.NoContent)
        }
    } else {
        // TM-037: GET /clubs/{id}/coach-link
        get("/clubs/{id}/coach-link") {
            val clubId = call.parameters["id"]!!
            requireClubManager(clubId)
            val link = coachLinkRepo.getActive(clubId)
                ?: throw NotFoundException("No active coach link for this club")
            call.respond(link)
        }

        // TM-038: POST /clubs/{id}/coach-link — rotate
        post("/clubs/{id}/coach-link") {
            val clubId = call.parameters["id"]!!
            requireClubManager(clubId)
            val uid = call.userId
            val request = call.receiveNullable<CreateCoachLinkRequest>() ?: CreateCoachLinkRequest()
            val link = coachLinkRepo.rotate(clubId, request, uid)
            call.respond(HttpStatusCode.Created, link)
        }

        // TM-039: DELETE /clubs/{id}/coach-link — revoke
        delete("/clubs/{id}/coach-link") {
            val clubId = call.parameters["id"]!!
            requireClubManager(clubId)
            coachLinkRepo.revoke(clubId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
