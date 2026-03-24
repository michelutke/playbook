package ch.teamorg.routes

import ch.teamorg.domain.repositories.AbwesenheitRepository
import ch.teamorg.domain.repositories.AbwesenheitRuleRow
import ch.teamorg.domain.repositories.CreateAbwesenheitRule
import ch.teamorg.domain.repositories.UpdateAbwesenheitRule
import ch.teamorg.infra.AbwesenheitBackfillJob
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.UUID

@Serializable
private data class CreateAbwesenheitRequest(
    val presetType: String,
    val label: String,
    val bodyPart: String? = null,
    val ruleType: String,
    val weekdays: List<Int>? = null,
    val startDate: String? = null,
    val endDate: String? = null
)

@Serializable
private data class UpdateAbwesenheitRequest(
    val presetType: String? = null,
    val label: String? = null,
    val bodyPart: String? = null,
    val ruleType: String? = null,
    val weekdays: List<Int>? = null,
    val startDate: String? = null,
    val endDate: String? = null
)

@Serializable
private data class AbwesenheitResponse(
    val id: String,
    val presetType: String,
    val label: String,
    val bodyPart: String?,
    val ruleType: String,
    val weekdays: List<Int>?,
    val startDate: String?,
    val endDate: String?,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
private data class BackfillStatusResponse(val status: String)

private fun AbwesenheitRuleRow.toResponse() = AbwesenheitResponse(
    id = id.toString(),
    presetType = presetType,
    label = label,
    bodyPart = bodyPart,
    ruleType = ruleType,
    weekdays = weekdays?.map { it.toInt() },
    startDate = startDate?.toString(),
    endDate = endDate?.toString(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString()
)

fun Route.abwesenheitRoutes() {
    val abwesenheitRepo by inject<AbwesenheitRepository>()
    val backfillJob by inject<AbwesenheitBackfillJob>()

    authenticate("jwt") {
        get("/users/me/abwesenheit") {
            val userId = UUID.fromString(call.principal<JWTPrincipal>()!!.payload.subject)
            val rules = abwesenheitRepo.listRules(userId)
            call.respond(rules.map { it.toResponse() })
        }

        post("/users/me/abwesenheit") {
            val userId = UUID.fromString(call.principal<JWTPrincipal>()!!.payload.subject)
            val body = call.receive<CreateAbwesenheitRequest>()
            val rule = abwesenheitRepo.createRule(
                userId,
                CreateAbwesenheitRule(
                    presetType = body.presetType,
                    label = body.label,
                    bodyPart = body.bodyPart,
                    ruleType = body.ruleType,
                    weekdays = body.weekdays?.map { it.toShort() },
                    startDate = body.startDate?.let { LocalDate.parse(it) },
                    endDate = body.endDate?.let { LocalDate.parse(it) }
                )
            )
            val app = application
            backfillJob.enqueue(userId, rule.id, app)
            call.respond(HttpStatusCode.Created, rule.toResponse())
        }

        put("/users/me/abwesenheit/{ruleId}") {
            val userId = UUID.fromString(call.principal<JWTPrincipal>()!!.payload.subject)
            val ruleId = UUID.fromString(call.parameters["ruleId"])
            val body = call.receive<UpdateAbwesenheitRequest>()
            val rule = abwesenheitRepo.updateRule(
                ruleId,
                UpdateAbwesenheitRule(
                    presetType = body.presetType,
                    label = body.label,
                    bodyPart = body.bodyPart,
                    ruleType = body.ruleType,
                    weekdays = body.weekdays?.map { it.toShort() },
                    startDate = body.startDate?.let { LocalDate.parse(it) },
                    endDate = body.endDate?.let { LocalDate.parse(it) }
                )
            )
            val app = application
            backfillJob.enqueue(userId, ruleId, app)
            call.respond(rule.toResponse())
        }

        delete("/users/me/abwesenheit/{ruleId}") {
            val ruleId = UUID.fromString(call.parameters["ruleId"])
            abwesenheitRepo.deleteRule(ruleId)
            call.respond(HttpStatusCode.NoContent)
        }

        get("/users/me/abwesenheit/backfill-status") {
            val userId = UUID.fromString(call.principal<JWTPrincipal>()!!.payload.subject)
            val status = backfillJob.getStatus(userId)
            call.respond(BackfillStatusResponse(status.name.lowercase()))
        }
    }
}
