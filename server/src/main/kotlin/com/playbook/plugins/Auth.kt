package com.playbook.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.playbook.db.tables.UsersTable
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

fun Application.configureAuth() {
    val jwtSecret = environment.config.property("jwt.secret").getString()
    val jwtIssuer = environment.config.property("jwt.issuer").getString()
    val jwtAudience = environment.config.property("jwt.audience").getString()

    install(Authentication) {
        // SA-006: regular user JWT
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

        // SA-006 / C-1 fix: super-admin JWT uses distinct "playbook-sa" audience.
        // Regular user tokens (audience "playbook-app") structurally cannot pass this verifier.
        jwt("super-admin") {
            realm = "Playbook SA"
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer(jwtIssuer)
                    .withAudience("playbook-sa")
                    .build()
            )
            validate { credential ->
                val userId = credential.payload.getClaim("sub").asString() ?: return@validate null
                val isSa = newSuspendedTransaction {
                    UsersTable.selectAll().where { UsersTable.id eq UUID.fromString(userId) }
                        .singleOrNull()?.get(UsersTable.superAdmin) ?: false
                }
                if (isSa) JWTPrincipal(credential.payload) else null
            }
        }

        // SA-008: impersonation JWT — separate audience, custom claims
        jwt("impersonation") {
            realm = "Playbook Impersonation"
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer(jwtIssuer)
                    .withAudience("playbook-impersonation")
                    .build()
            )
            validate { credential ->
                val userId = credential.payload.getClaim("sub").asString() ?: return@validate null
                val impersonatedBy = credential.payload.getClaim("impersonated_by").asString()
                val sessionId = credential.payload.getClaim("impersonation_session_id").asString()
                if (userId != null && impersonatedBy != null && sessionId != null)
                    JWTPrincipal(credential.payload) else null
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
