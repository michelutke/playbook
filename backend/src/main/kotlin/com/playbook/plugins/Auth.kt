package com.playbook.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureAuth() {
    val jwtSecret = environment.config.property("jwt.secret").getString()
    val jwtIssuer = environment.config.property("jwt.issuer").getString()
    val jwtAudience = environment.config.property("jwt.audience").getString()

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "Playbook"
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer(jwtIssuer)
                    .withAudience(jwtAudience)
                    .build()
            )
            validate { credential ->
                val userId = credential.payload.getClaim("sub").asString()
                if (userId != null) JWTPrincipal(credential.payload) else null
            }
        }
    }
}

/** Extract the authenticated user's UUID from the JWT principal. */
val ApplicationCall.userId: String
    get() = principal<JWTPrincipal>()!!.payload.getClaim("sub").asString()

/** Extract userId or return null (for optional auth contexts). */
val ApplicationCall.userIdOrNull: String?
    get() = principal<JWTPrincipal>()?.payload?.getClaim("sub")?.asString()
