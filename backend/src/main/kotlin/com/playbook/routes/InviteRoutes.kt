package com.playbook.routes

import com.playbook.domain.CreateInviteRequest
import com.playbook.middleware.requireCoachOnTeam
import com.playbook.plugins.userId
import com.playbook.plugins.userIdOrNull
import com.playbook.repository.InviteRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.registerInviteRoutes(authenticated: Boolean) {
    val inviteRepo: InviteRepository by inject()

    if (!authenticated) {
        // TM-035: GET /invites/{token} — public
        get("/invites/{token}") {
            val token = call.parameters["token"]!!
            val context = inviteRepo.resolveToken(token)
            if (context == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Invite not found"))
                return@get
            }
            call.respond(context)
        }

        // TM-036: POST /invites/{token}/accept — public (user may or may not be authenticated)
        post("/invites/{token}/accept") {
            val token = call.parameters["token"]!!
            val uid = call.userIdOrNull ?: throw IllegalArgumentException("Authentication required to accept invite")
            inviteRepo.accept(token, uid)
            call.respond(HttpStatusCode.NoContent)
        }
    } else {
        // TM-032: POST /teams/{id}/invites
        post("/teams/{id}/invites") {
            val teamId = call.parameters["id"]!!
            requireCoachOnTeam(teamId)
            val uid = call.userId
            val request = call.receive<CreateInviteRequest>()
            val invite = inviteRepo.create(teamId, request, uid)
            call.respond(HttpStatusCode.Created, invite)
        }

        // TM-033: GET /teams/{id}/invites
        get("/teams/{id}/invites") {
            val teamId = call.parameters["id"]!!
            requireCoachOnTeam(teamId)
            val invites = inviteRepo.listPending(teamId)
            call.respond(invites)
        }

        // TM-034: DELETE /teams/{id}/invites/{inviteId}
        delete("/teams/{id}/invites/{inviteId}") {
            val teamId = call.parameters["id"]!!
            requireCoachOnTeam(teamId)
            val inviteId = call.parameters["inviteId"]!!
            inviteRepo.revoke(inviteId, teamId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
