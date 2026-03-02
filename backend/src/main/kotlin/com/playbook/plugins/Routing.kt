package com.playbook.plugins

import com.playbook.plugins.AuditPlugin
import com.playbook.routes.registerClubRoutes
import com.playbook.routes.registerCoachLinkRoutes
import com.playbook.routes.registerEventRoutes
import com.playbook.routes.registerInviteRoutes
import com.playbook.routes.registerMemberRoutes
import com.playbook.routes.registerSubgroupRoutes
import com.playbook.routes.registerSuperAdminRoutes
import com.playbook.routes.registerTeamRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val allowedHost = environment.config.propertyOrNull("cors.allowedHost")?.getString() ?: "localhost:3000"
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHost(allowedHost, schemes = listOf("https", "http"))
    }

    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to (cause.message ?: "Bad request")))
        }
        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, mapOf("error" to (cause.message ?: "Not found")))
        }
        exception<ForbiddenException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to (cause.message ?: "Forbidden")))
        }
        exception<ConflictException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, mapOf("error" to (cause.message ?: "Conflict")))
        }
        exception<GoneException> { call, cause ->
            call.respond(HttpStatusCode.Gone, mapOf("error" to (cause.message ?: "Gone")))
        }
    }

    routing {
        // Public routes (no auth)
        registerInviteRoutes(authenticated = false)
        registerCoachLinkRoutes(authenticated = false)

        // Protected routes
        authenticate("auth-jwt") {
            registerClubRoutes()
            registerTeamRoutes()
            registerMemberRoutes()
            registerInviteRoutes(authenticated = true)
            registerCoachLinkRoutes(authenticated = true)
            registerEventRoutes()
            registerSubgroupRoutes()
        }

        // Super-admin routes (SA-006, SA-010)
        authenticate("super-admin") {
            install(AuditPlugin)
            registerSuperAdminRoutes()
        }
    }
}

// Domain exceptions
class NotFoundException(message: String) : Exception(message)
class ForbiddenException(message: String) : Exception(message)
class ConflictException(message: String) : Exception(message)
class GoneException(message: String) : Exception(message)
