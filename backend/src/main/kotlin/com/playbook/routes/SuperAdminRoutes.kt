package com.playbook.routes

import com.playbook.db.repositories.SuperAdminRepositoryImpl
import com.playbook.domain.sa.*
import com.playbook.middleware.mintImpersonationToken
import com.playbook.middleware.requireSuperAdmin
import com.playbook.plugins.NotFoundException
import com.playbook.plugins.userId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.io.File

fun Route.registerSuperAdminRoutes() {
    val repo: SuperAdminRepositoryImpl by inject()
    val jwtSecret by lazy { application.environment.config.property("jwt.secret").getString() }
    val jwtIssuer by lazy { application.environment.config.property("jwt.issuer").getString() }
    val baseUrl by lazy {
        application.environment.config.propertyOrNull("app.baseUrl")?.getString() ?: "https://app.playbook.example"
    }
    val billingRateChf by lazy {
        application.environment.config.propertyOrNull("billing.rateChf")?.getString()?.toDouble() ?: 1.0
    }

    route("/sa") {
        // SA-012
        get("/stats") {
            requireSuperAdmin()
            call.respond(repo.getStats())
        }

        // SA-013
        get("/clubs") {
            requireSuperAdmin()
            val status = call.request.queryParameters["status"]
            val search = call.request.queryParameters["search"]
            call.respond(repo.listClubs(status, search))
        }

        // SA-014
        post("/clubs") {
            val saId = requireSuperAdmin()
            val request = call.receive<CreateSaClubRequest>()
            if (request.name.isBlank()) throw IllegalArgumentException("Club name is required")
            val club = repo.createClub(request, saId)
            call.respond(HttpStatusCode.Created, club)
        }

        // SA-015
        get("/clubs/{id}") {
            requireSuperAdmin()
            val clubId = call.parameters["id"]!!
            val club = repo.getClub(clubId) ?: throw NotFoundException("Club not found")
            call.respond(club)
        }

        // SA-016
        patch("/clubs/{id}") {
            requireSuperAdmin()
            val clubId = call.parameters["id"]!!
            val request = call.receive<UpdateSaClubRequest>()
            val club = repo.updateClub(clubId, request)
            call.respond(club)
        }

        // SA-017
        post("/clubs/{id}/deactivate") {
            requireSuperAdmin()
            val clubId = call.parameters["id"]!!
            repo.deactivateClub(clubId)
            call.respond(HttpStatusCode.NoContent)
        }

        // SA-018
        post("/clubs/{id}/reactivate") {
            requireSuperAdmin()
            val clubId = call.parameters["id"]!!
            repo.reactivateClub(clubId)
            call.respond(HttpStatusCode.NoContent)
        }

        // SA-019
        delete("/clubs/{id}") {
            requireSuperAdmin()
            val clubId = call.parameters["id"]!!
            val request = call.receive<DeleteClubRequest>()
            repo.deleteClub(clubId, request.confirmName)
            call.respond(HttpStatusCode.NoContent)
        }

        // SA-020
        get("/clubs/{id}/managers") {
            requireSuperAdmin()
            val clubId = call.parameters["id"]!!
            call.respond(repo.listManagers(clubId))
        }

        // SA-021
        post("/clubs/{id}/managers") {
            val saId = requireSuperAdmin()
            val clubId = call.parameters["id"]!!
            val request = call.receive<InviteManagerRequest>()
            if (request.email.isBlank()) throw IllegalArgumentException("Email is required")
            val manager = repo.inviteManager(clubId, request.email, saId)
            call.respond(HttpStatusCode.Created, manager)
        }

        // SA-022
        delete("/clubs/{id}/managers/{managerId}") {
            requireSuperAdmin()
            val clubId = call.parameters["id"]!!
            val managerId = call.parameters["managerId"]!!
            repo.removeManager(clubId, managerId)
            call.respond(HttpStatusCode.NoContent)
        }

        // SA-023: start impersonation
        post("/clubs/{id}/managers/{managerId}/impersonate") {
            val saId = requireSuperAdmin()
            val clubId = call.parameters["id"]!!
            val managerId = call.parameters["managerId"]!!

            // Resolve user_id from manager row
            val managers = repo.listManagers(clubId)
            val target = managers.find { it.id == managerId }
                ?: throw NotFoundException("Manager not found")
            val managerUserId = target.userId
                ?: throw IllegalArgumentException("Manager has not accepted invite yet")

            val (sessionId, expiresAt) = repo.startImpersonationSession(saId, clubId, managerUserId)
            val (token, _) = call.mintImpersonationToken(
                saId, managerUserId, sessionId.toString(), jwtSecret, jwtIssuer
            )
            call.respond(
                ImpersonationResponse(
                    token = token,
                    sessionId = sessionId.toString(),
                    expiresAt = expiresAt.toInstant().toString(),
                )
            )
        }

        // SA-024: end impersonation
        post("/impersonation/{sessionId}/end") {
            requireSuperAdmin()
            val sessionId = call.parameters["sessionId"]!!
            repo.endImpersonation(sessionId)
            call.respond(HttpStatusCode.NoContent)
        }

        // SA-025
        get("/users/search") {
            requireSuperAdmin()
            val q = call.request.queryParameters["q"] ?: throw IllegalArgumentException("q parameter required")
            call.respond(repo.searchUsers(q))
        }

        // SA-026
        get("/audit-log") {
            requireSuperAdmin()
            val actorId = call.request.queryParameters["actorId"]
            val action = call.request.queryParameters["action"]
            val from = call.request.queryParameters["from"]
            val to = call.request.queryParameters["to"]
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = (call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 50).coerceAtMost(200)
            call.respond(repo.listAuditLog(actorId, action, from, to, page, pageSize))
        }

        // SA-027
        get("/audit-log/{id}") {
            requireSuperAdmin()
            val id = call.parameters["id"]!!
            val entry = repo.getAuditLogEntry(id) ?: throw NotFoundException("Audit log entry not found")
            call.respond(entry)
        }

        // SA-028
        post("/audit-log/export") {
            val saId = requireSuperAdmin()
            val filters = call.request.queryParameters.entries().associate { it.key to it.value.first() }
                .filterKeys { it != "page" && it != "pageSize" }
                .let { if (it.isEmpty()) null else kotlinx.serialization.json.Json.encodeToString(it) }
            val jobId = repo.enqueueExportJob(saId, filters)
            call.respond(HttpStatusCode.Accepted, ExportJobResponse(jobId))
        }

        // SA-029: poll status
        get("/audit-log/export/{jobId}") {
            requireSuperAdmin()
            val jobId = call.parameters["jobId"]!!
            val status = repo.getExportJob(jobId, baseUrl)
                ?: throw NotFoundException("Export job not found")
            call.respond(status)
        }

        // SA-033: download export file
        get("/audit-log/export/{jobId}/download") {
            requireSuperAdmin()
            val jobId = call.parameters["jobId"]!!
            val path = repo.getExportFilePath(jobId) ?: throw NotFoundException("Export file not found")
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "audit-log-$jobId.csv")
                    .toString()
            )
            call.respondFile(File(path))
        }

        // SA-030
        get("/clubs/{id}/members") {
            requireSuperAdmin()
            val clubId = call.parameters["id"]!!
            call.respond(repo.listClubMembers(clubId))
        }

        // SA-031
        get("/billing/summary") {
            requireSuperAdmin()
            call.respond(repo.getBillingSummary(billingRateChf))
        }
    }
}
