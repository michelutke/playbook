package com.playbook.routes

import com.playbook.domain.CreateSubgroupRequest
import com.playbook.domain.UpdateSubgroupRequest
import com.playbook.middleware.requireCoachOnSubgroupTeam
import com.playbook.middleware.requireCoachOnTeam
import com.playbook.plugins.NotFoundException
import com.playbook.repository.SubgroupRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.registerSubgroupRoutes() {
    val subgroupRepo: SubgroupRepository by inject()

    // ES-016: GET /teams/{teamId}/subgroups
    get("/teams/{teamId}/subgroups") {
        val teamId = call.parameters["teamId"]!!
        requireCoachOnTeam(teamId)
        val subgroups = subgroupRepo.listForTeam(teamId)
        call.respond(subgroups)
    }

    // ES-017: POST /teams/{teamId}/subgroups
    post("/teams/{teamId}/subgroups") {
        val teamId = call.parameters["teamId"]!!
        requireCoachOnTeam(teamId)
        val request = call.receive<CreateSubgroupRequest>()
        val subgroup = subgroupRepo.create(teamId, request)
        call.respond(HttpStatusCode.Created, subgroup)
    }

    // ES-018: PATCH /subgroups/{id}
    patch("/subgroups/{id}") {
        val id = call.parameters["id"]!!
        requireCoachOnSubgroupTeam(id)
        val request = call.receive<UpdateSubgroupRequest>()
        val subgroup = subgroupRepo.update(id, request)
        call.respond(subgroup)
    }

    // ES-019: DELETE /subgroups/{id}
    delete("/subgroups/{id}") {
        val id = call.parameters["id"]!!
        requireCoachOnSubgroupTeam(id)
        subgroupRepo.delete(id)
        call.respond(HttpStatusCode.NoContent)
    }
}
