package com.playbook.routes

import com.playbook.domain.repositories.InviteRepository
import com.playbook.domain.repositories.TeamRepository
import com.playbook.domain.repositories.UserRepository
import com.playbook.middleware.authenticateUser
import com.playbook.middleware.requireTeamRole
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import java.time.LocalDateTime
import java.util.*

@Serializable
data class CreateInviteRequest(val role: String, val email: String? = null)

@Serializable
data class InviteResponse(val token: String, val inviteUrl: String, val expiresAt: String)

fun Route.inviteRoutes() {
    val inviteRepository by inject<InviteRepository>()
    val teamRepository by inject<TeamRepository>()
    val userRepository by inject<UserRepository>()

    route("/teams/{teamId}/invites") {
        authenticate("jwt") {
            post {
                val teamId = call.parameters["teamId"]?.let { UUID.fromString(it) }
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid team ID")

                // Coach or ClubManager required
                if (!call.requireTeamRole(teamId, "coach", "club_manager", teamRepository = teamRepository)) {
                    return@post
                }

                val request = call.receive<CreateInviteRequest>()
                if (request.role !in listOf("player", "coach")) {
                    return@post call.respond(HttpStatusCode.BadRequest, "Invalid role. Must be 'player' or 'coach'")
                }

                authenticateUser(userRepository) { user ->
                    val invite = inviteRepository.create(
                        teamId = teamId,
                        createdByUserId = UUID.fromString(user.id),
                        role = request.role,
                        email = request.email
                    )

                    val inviteUrl = "playbook://invite/team/${invite.token}"
                    call.respond(HttpStatusCode.Created, InviteResponse(
                        token = invite.token,
                        inviteUrl = inviteUrl,
                        expiresAt = invite.expiresAt
                    ))
                }
            }
        }
    }

    route("/invites/{token}") {
        // GET details - public, no auth
        get {
            val token = call.parameters["token"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing token")
            val details = inviteRepository.getInviteDetails(token)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Invite not found")

            call.respond(details)
        }

        // POST redeem - auth required
        authenticate("jwt") {
            post("/redeem") {
                val token = call.parameters["token"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing token")
                val invite = inviteRepository.findByToken(token)
                    ?: return@post call.respond(HttpStatusCode.NotFound, "Invite not found")

                // Check expiry
                val expiresAt = LocalDateTime.parse(invite.expiresAt)
                if (LocalDateTime.now().isAfter(expiresAt)) {
                    return@post call.respond(HttpStatusCode.Gone, "Invite link has expired")
                }

                // Check if already redeemed by someone else
                authenticateUser(userRepository) { user ->
                    val userId = UUID.fromString(user.id)
                    
                    if (invite.redeemedAt != null && invite.redeemedByUserId != user.id) {
                        return@authenticateUser call.respond(HttpStatusCode.Conflict, "Invite already redeemed by another user")
                    }

                    val redeemedInvite = inviteRepository.redeem(token, userId)
                    call.respond(HttpStatusCode.OK, redeemedInvite)
                }
            }
        }
    }
}
