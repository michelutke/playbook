package com.playbook.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.playbook.domain.repositories.UserRepository
import com.playbook.middleware.authenticateUser
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.mindrot.jbcrypt.BCrypt
import org.koin.ktor.ext.inject
import java.util.*

@Serializable
data class RegisterRequest(val email: String, val password: String, val displayName: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val userId: String, val displayName: String, val avatarUrl: String?)

fun Route.authRoutes() {
    val userRepository by inject<UserRepository>()
    
    val jwtSecret = application.environment.config.property("jwt.secret").getString()
    val jwtIssuer = application.environment.config.property("jwt.issuer").getString()
    val jwtAudience = application.environment.config.property("jwt.audience").getString()
    val expiryDays = application.environment.config.property("jwt.expiry-days").getString().toLong()

    fun generateToken(userId: String): String {
        return JWT.create()
            .withAudience(jwtAudience)
            .withIssuer(jwtIssuer)
            .withSubject(userId)
            .withExpiresAt(Date(System.currentTimeMillis() + expiryDays * 24 * 60 * 60 * 1000))
            .sign(Algorithm.HMAC256(jwtSecret))
    }

    route("/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            
            if (!isValidEmail(request.email)) {
                return@post call.respond(HttpStatusCode.BadRequest, "Invalid email format")
            }
            if (request.password.length < 8) {
                return@post call.respond(HttpStatusCode.BadRequest, "Password must be at least 8 characters")
            }
            if (request.displayName.isBlank()) {
                return@post call.respond(HttpStatusCode.BadRequest, "Display name cannot be empty")
            }

            if (userRepository.existsByEmail(request.email)) {
                return@post call.respond(HttpStatusCode.Conflict, "Email already registered")
            }

            val passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt(12))
            val user = userRepository.create(request.email, passwordHash, request.displayName)
            
            val token = generateToken(user.id)
            call.respond(AuthResponse(token, user.id, user.displayName, user.avatarUrl))
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val passwordHash = userRepository.getPasswordHash(request.email)
            
            if (passwordHash == null || !BCrypt.checkpw(request.password, passwordHash)) {
                return@post call.respond(HttpStatusCode.Unauthorized, "Invalid email or password")
            }

            val user = userRepository.findByEmail(request.email)!!
            val token = generateToken(user.id)
            call.respond(AuthResponse(token, user.id, user.displayName, user.avatarUrl))
        }

        authenticate("jwt") {
            post("/logout") {
                // Stateless JWT logout - 200 OK
                call.respond(HttpStatusCode.OK)
            }

            get("/me") {
                call.authenticateUser(userRepository) { user ->
                    call.respond(user)
                }
            }
        }
    }
}

private fun isValidEmail(email: String): Boolean {
    return email.contains("@") && email.indexOf("@") < email.lastIndexOf(".")
}
