package com.playbook.routes

import com.playbook.domain.AddRoleRequest
import com.playbook.domain.UpdatePlayerProfileRequest
import com.playbook.middleware.requireClubManager
import com.playbook.middleware.requireCoachOnTeam
import com.playbook.plugins.NotFoundException
import com.playbook.plugins.userId
import com.playbook.repository.MembershipRepository
import com.playbook.repository.TeamRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.registerMemberRoutes() {
    val memberRepo: MembershipRepository by inject()
    val teamRepo: TeamRepository by inject()

    // TM-025: GET /teams/{id}/members
    get("/teams/{id}/members") {
        val teamId = call.parameters["id"]!!
        requireCoachOnTeam(teamId)
        val roster = memberRepo.getRoster(teamId)
        call.respond(roster)
    }

    // TM-026: POST /teams/{id}/members/{userId}/roles
    post("/teams/{id}/members/{userId}/roles") {
        val teamId = call.parameters["id"]!!
        val targetUserId = call.parameters["userId"]!!
        val team = teamRepo.getById(teamId) ?: throw NotFoundException("Team not found")
        requireClubManager(team.clubId)
        val request = call.receive<AddRoleRequest>()
        val uid = call.userId
        memberRepo.addRole(teamId, targetUserId, request.role, uid)
        call.respond(HttpStatusCode.NoContent)
    }

    // TM-027: DELETE /teams/{id}/members/{userId}/roles/{role}
    delete("/teams/{id}/members/{userId}/roles/{role}") {
        val teamId = call.parameters["id"]!!
        val targetUserId = call.parameters["userId"]!!
        val roleStr = call.parameters["role"]!!
        val team = teamRepo.getById(teamId) ?: throw NotFoundException("Team not found")
        requireClubManager(team.clubId)
        val role = com.playbook.domain.MemberRole.valueOf(roleStr.uppercase())
        memberRepo.removeRole(teamId, targetUserId, role)
        call.respond(HttpStatusCode.NoContent)
    }

    // TM-028: DELETE /teams/{id}/members/{userId}
    delete("/teams/{id}/members/{userId}") {
        val teamId = call.parameters["id"]!!
        val targetUserId = call.parameters["userId"]!!
        val team = teamRepo.getById(teamId) ?: throw NotFoundException("Team not found")
        requireClubManager(team.clubId)
        memberRepo.removeMember(teamId, targetUserId)
        call.respond(HttpStatusCode.NoContent)
    }

    // TM-029: DELETE /users/me/teams/{teamId}
    delete("/users/me/teams/{teamId}") {
        val teamId = call.parameters["teamId"]!!
        val uid = call.userId
        memberRepo.leaveTeam(teamId, uid)
        call.respond(HttpStatusCode.NoContent)
    }

    // TM-030: GET + PATCH /teams/{id}/members/{userId}/profile
    get("/teams/{id}/members/{userId}/profile") {
        val teamId = call.parameters["id"]!!
        val targetUserId = call.parameters["userId"]!!
        requireCoachOnTeam(teamId)
        val profile = memberRepo.getProfile(teamId, targetUserId)
            ?: com.playbook.domain.PlayerProfile(teamId, targetUserId, null, null)
        call.respond(profile)
    }

    patch("/teams/{id}/members/{userId}/profile") {
        val teamId = call.parameters["id"]!!
        val targetUserId = call.parameters["userId"]!!
        requireCoachOnTeam(teamId)
        val request = call.receive<UpdatePlayerProfileRequest>()
        val profile = memberRepo.updateProfile(teamId, targetUserId, request)
        call.respond(profile)
    }
}
