package com.playbook.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.playbook.db.tables.UsersTable
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.util.Date
import java.util.UUID

@Serializable
data class SaLoginRequest(val email: String, val password: String)

@Serializable
data class SaLoginResponse(val token: String)

/**
 * SA-006 / C-1 fix: dedicated SA login that issues JWTs with audience "playbook-sa".
 * Regular user tokens (audience "playbook-app") cannot authenticate SA routes.
 *
 * Authentication: email + SA master password from `sa.password` config.
 * The SA master password is set via SA_PASSWORD env var in production.
 */
fun Route.registerSaAuthRoutes() {
    val jwtSecret by lazy { application.environment.config.property("jwt.secret").getString() }
    val jwtIssuer by lazy { application.environment.config.property("jwt.issuer").getString() }
    val saPassword by lazy {
        application.environment.config.propertyOrNull("sa.password")?.getString() ?: ""
    }

    post("/auth/sa-login") {
        val request = call.receive<SaLoginRequest>()

        if (saPassword.isBlank()) {
            call.respond(HttpStatusCode.ServiceUnavailable, mapOf("error" to "SA login not configured"))
            return@post
        }
        if (request.password != saPassword) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
            return@post
        }

        val user = newSuspendedTransaction {
            UsersTable.selectAll().where { UsersTable.email eq request.email }.singleOrNull()
        }
        if (user == null || !user[UsersTable.superAdmin]) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
            return@post
        }

        val expiresAt = Instant.now().plusSeconds(3600 * 24) // 24h SA session
        val token = JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience("playbook-sa")
            .withSubject(user[UsersTable.id].toString())
            .withExpiresAt(Date.from(expiresAt))
            .sign(Algorithm.HMAC256(jwtSecret))

        call.respond(SaLoginResponse(token))
    }
}
