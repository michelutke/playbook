package com.playbook.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.playbook.db.tables.ClubManagersTable
import com.playbook.db.tables.UsersTable
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.insert
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Date
import java.util.UUID

@Serializable
data class RegisterRequest(val email: String, val password: String, val displayName: String? = null)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val userId: String)

@Serializable
data class UserProfileResponse(val id: String, val email: String, val displayName: String?, val clubId: String?)

fun Route.registerAuthRoutes() {
    val jwtSecret by lazy { application.environment.config.property("jwt.secret").getString() }
    val jwtIssuer by lazy { application.environment.config.property("jwt.issuer").getString() }
    val jwtAudience by lazy { application.environment.config.property("jwt.audience").getString() }

    post("/auth/register") {
        val req = call.receive<RegisterRequest>()
        if (req.email.isBlank() || req.password.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "email and password required"))
            return@post
        }
        val hash = BCrypt.hashpw(req.password, BCrypt.gensalt())
        val userId = newSuspendedTransaction {
            val existing = UsersTable.selectAll().where { UsersTable.email eq req.email }.singleOrNull()
            if (existing != null) {
                UsersTable.update({ UsersTable.email eq req.email }) {
                    it[passwordHash] = hash
                    if (req.displayName != null) it[displayName] = req.displayName
                }
                existing[UsersTable.id].toString()
            } else {
                val newId = UUID.randomUUID()
                val now = OffsetDateTime.now(ZoneOffset.UTC)
                UsersTable.insert {
                    it[id] = newId
                    it[email] = req.email
                    it[passwordHash] = hash
                    it[displayName] = req.displayName
                    it[createdAt] = now
                    it[updatedAt] = now
                }
                newId.toString()
            }
        }
        val token = issueToken(jwtSecret, jwtIssuer, jwtAudience, userId)
        call.respond(HttpStatusCode.Created, AuthResponse(token = token, userId = userId))
    }

    post("/auth/login") {
        val req = call.receive<LoginRequest>()
        val result = newSuspendedTransaction {
            val row = UsersTable.selectAll().where { UsersTable.email eq req.email }.singleOrNull()
                ?: return@newSuspendedTransaction null
            Pair(row[UsersTable.id].toString(), row[UsersTable.passwordHash])
        }
        if (result == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid credentials"))
            return@post
        }
        val (userId, storedHash) = result
        if (storedHash == null || !BCrypt.checkpw(req.password, storedHash)) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid credentials"))
            return@post
        }
        val token = issueToken(jwtSecret, jwtIssuer, jwtAudience, userId)
        call.respond(AuthResponse(token = token, userId = userId))
    }
}

fun Route.registerUsersMeRoute() {
    get("/users/me") {
        val userId = call.principal<JWTPrincipal>()?.payload?.subject
            ?: run {
                call.respond(HttpStatusCode.Unauthorized)
                return@get
            }
        val profile = newSuspendedTransaction {
            val row = UsersTable.selectAll().where { UsersTable.id eq UUID.fromString(userId) }.singleOrNull()
                ?: return@newSuspendedTransaction null
            val clubId = ClubManagersTable.selectAll()
                .where { ClubManagersTable.userId eq UUID.fromString(userId) }
                .singleOrNull()
                ?.get(ClubManagersTable.clubId)
                ?.toString()
            UserProfileResponse(
                id = userId,
                email = row[UsersTable.email],
                displayName = row[UsersTable.displayName],
                clubId = clubId
            )
        }
        if (profile == null) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        call.respond(profile)
    }
}

private fun issueToken(secret: String, issuer: String, audience: String, userId: String): String =
    JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withSubject(userId)
        .withExpiresAt(Date.from(Instant.now().plusSeconds(86_400L)))
        .sign(Algorithm.HMAC256(secret))
