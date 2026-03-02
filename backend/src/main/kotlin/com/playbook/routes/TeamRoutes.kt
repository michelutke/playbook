package com.playbook.routes

import com.playbook.domain.CreateTeamRequest
import com.playbook.domain.RejectTeamRequest
import com.playbook.domain.TeamStatus
import com.playbook.domain.UpdateTeamRequest
import com.playbook.middleware.requireClubManager
import com.playbook.middleware.requireCoachOnTeam
import com.playbook.plugins.NotFoundException
import com.playbook.plugins.userId
import com.playbook.repository.TeamRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.registerTeamRoutes() {
    val teamRepo: TeamRepository by inject()

    // TM-017: GET /clubs/{clubId}/teams
    get("/clubs/{clubId}/teams") {
        val clubId = call.parameters["clubId"]!!
        requireClubManager(clubId)
        val teams = teamRepo.listByClub(clubId)
        call.respond(teams)
    }

    // TM-018: POST /clubs/{clubId}/teams (ClubManager creates team directly)
    post("/clubs/{clubId}/teams") {
        val clubId = call.parameters["clubId"]!!
        requireClubManager(clubId)
        val request = call.receive<CreateTeamRequest>()
        val team = teamRepo.create(clubId, request)
        call.respond(HttpStatusCode.Created, team)
    }

    // TM-019: POST /clubs/{clubId}/teams/request (Coach submits for approval)
    post("/clubs/{clubId}/teams/request") {
        val clubId = call.parameters["clubId"]!!
        val uid = call.userId
        val request = call.receive<CreateTeamRequest>()
        val team = teamRepo.submitRequest(clubId, request, uid)
        call.respond(HttpStatusCode.Created, team)
    }

    // TM-020: GET /teams/{id}
    get("/teams/{id}") {
        val id = call.parameters["id"]!!
        val team = teamRepo.getById(id) ?: throw NotFoundException("Team not found")
        call.respond(team)
    }

    // TM-021: PATCH /teams/{id}
    patch("/teams/{id}") {
        val id = call.parameters["id"]!!
        requireCoachOnTeam(id)
        val request = call.receive<UpdateTeamRequest>()
        val team = teamRepo.update(id, request)
        call.respond(team)
    }

    // TM-022: POST /teams/{id}/archive + unarchive
    post("/teams/{id}/archive") {
        val id = call.parameters["id"]!!
        val team = teamRepo.getById(id) ?: throw NotFoundException("Team not found")
        requireClubManager(team.clubId)
        val archived = teamRepo.setStatus(id, TeamStatus.ARCHIVED)
        call.respond(archived)
    }

    post("/teams/{id}/unarchive") {
        val id = call.parameters["id"]!!
        val team = teamRepo.getById(id) ?: throw NotFoundException("Team not found")
        requireClubManager(team.clubId)
        val active = teamRepo.setStatus(id, TeamStatus.ACTIVE)
        call.respond(active)
    }

    // TM-023: DELETE /teams/{id}
    delete("/teams/{id}") {
        val id = call.parameters["id"]!!
        val team = teamRepo.getById(id) ?: throw NotFoundException("Team not found")
        requireClubManager(team.clubId)
        teamRepo.delete(id)
        call.respond(HttpStatusCode.NoContent)
    }

    // TM-024: POST /teams/{id}/approve + reject
    post("/teams/{id}/approve") {
        val id = call.parameters["id"]!!
        val team = teamRepo.getById(id) ?: throw NotFoundException("Team not found")
        requireClubManager(team.clubId)
        val approved = teamRepo.approve(id)
        call.respond(approved)
    }

    post("/teams/{id}/reject") {
        val id = call.parameters["id"]!!
        val team = teamRepo.getById(id) ?: throw NotFoundException("Team not found")
        requireClubManager(team.clubId)
        val request = call.receiveNullable<RejectTeamRequest>() ?: RejectTeamRequest()
        val rejected = teamRepo.reject(id, request)
        call.respond(rejected)
    }
}
