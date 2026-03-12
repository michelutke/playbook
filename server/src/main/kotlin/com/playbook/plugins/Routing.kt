package com.playbook.plugins

import com.playbook.routes.authRoutes
import com.playbook.routes.clubRoutes
import com.playbook.routes.teamRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        authRoutes()
        clubRoutes()
        teamRoutes()
    }
}
