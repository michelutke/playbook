package com.playbook.routes

import com.playbook.domain.CreateAbwesenheitRuleRequest
import com.playbook.domain.UpdateAbwesenheitRuleRequest
import com.playbook.infra.launchNotifyAbwesenheitChange
import com.playbook.plugins.userId
import com.playbook.push.NotificationService
import com.playbook.repository.AbwesenheitRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.registerAbwesenheitRoutes() {
    val abwesenheitRepo: AbwesenheitRepository by inject()
    val notificationService: NotificationService by inject()

    // T-008: GET /users/me/abwesenheit
    get("/users/me/abwesenheit") {
        val uid = call.userId
        val rules = abwesenheitRepo.listRules(uid)
        call.respond(rules)
    }

    // T-009: POST /users/me/abwesenheit
    post("/users/me/abwesenheit") {
        val uid = call.userId
        val request = call.receive<CreateAbwesenheitRuleRequest>()
        val result = abwesenheitRepo.createRule(uid, request)
        // NT-041: notify user of abwesenheit rule creation
        launchNotifyAbwesenheitChange(UUID.fromString(result.rule.id), notificationService)
        call.respond(HttpStatusCode.Created, result)
    }

    // T-010: PUT /users/me/abwesenheit/{ruleId}
    put("/users/me/abwesenheit/{ruleId}") {
        val ruleId = call.parameters["ruleId"]!!
        val uid = call.userId
        val request = call.receive<UpdateAbwesenheitRuleRequest>()
        val rule = abwesenheitRepo.updateRule(ruleId, uid, request)
        // NT-041: notify user of abwesenheit rule update
        launchNotifyAbwesenheitChange(UUID.fromString(rule.id), notificationService)
        call.respond(rule)
    }

    // T-011: DELETE /users/me/abwesenheit/{ruleId}
    delete("/users/me/abwesenheit/{ruleId}") {
        val ruleId = call.parameters["ruleId"]!!
        val uid = call.userId
        abwesenheitRepo.deleteRule(ruleId, uid)
        call.respond(HttpStatusCode.NoContent)
    }

    // T-012: GET /users/me/abwesenheit/backfill-status
    get("/users/me/abwesenheit/backfill-status") {
        val uid = call.userId
        val status = abwesenheitRepo.getBackfillStatus(uid)
        call.respond(status)
    }
}
