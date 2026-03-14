package com.playbook.plugins

import com.playbook.routes.authRoutes
import com.playbook.routes.clubRoutes
import com.playbook.routes.inviteRoutes
import com.playbook.routes.teamRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, "OK")
        }
        authRoutes()
        clubRoutes()
        teamRoutes()
        inviteRoutes()
    }
}
