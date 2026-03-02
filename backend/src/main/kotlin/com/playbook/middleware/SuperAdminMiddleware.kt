package com.playbook.middleware

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.playbook.db.tables.UsersTable
import com.playbook.plugins.ForbiddenException
import com.playbook.plugins.userId
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.util.Date
import java.util.UUID

// SA-007: verify super_admin flag in DB
suspend fun RoutingContext.requireSuperAdmin(): String {
    val uid = call.userId
    val isSa = newSuspendedTransaction {
        UsersTable.select { UsersTable.id eq UUID.fromString(uid) }
            .singleOrNull()?.get(UsersTable.superAdmin) ?: false
    }
    if (!isSa) throw ForbiddenException("Super-admin access required")
    return uid
}

// SA-009: mint short-lived impersonation JWT (max 3600s)
fun ApplicationCall.mintImpersonationToken(
    saId: String,
    managerId: String,
    sessionId: String,
    jwtSecret: String,
    jwtIssuer: String,
): Pair<String, Instant> {
    val expiresAt = Instant.now().plusSeconds(3600)
    val token = JWT.create()
        .withIssuer(jwtIssuer)
        .withAudience("playbook-impersonation")
        .withSubject(managerId)
        .withClaim("role", "club_manager")
        .withClaim("impersonated_by", saId)
        .withClaim("impersonation_session_id", sessionId)
        .withExpiresAt(Date.from(expiresAt))
        .sign(Algorithm.HMAC256(jwtSecret))
    return Pair(token, expiresAt)
}
