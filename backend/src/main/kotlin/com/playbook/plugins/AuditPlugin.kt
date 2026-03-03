package com.playbook.plugins

import com.playbook.db.tables.AuditLogTable
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * SA-010/SA-011: Audit logging plugin for /sa/{route} routes.
 * Writes an audit_log row after each mutating SA request.
 * Detects impersonation JWT claims and records impersonated_as.
 */
val AuditPlugin = createRouteScopedPlugin("AuditPlugin") {
    onCallRespond { call, _ ->
        val method = call.request.httpMethod.value
        if (method == "GET" || method == "OPTIONS") return@onCallRespond

        val principal = call.principal<JWTPrincipal>() ?: return@onCallRespond
        val actorId = principal.payload.getClaim("sub").asString() ?: return@onCallRespond

        // SA-011: detect impersonation claims
        val impersonatedBy = principal.payload.getClaim("impersonated_by")?.asString()
        val sessionId = principal.payload.getClaim("impersonation_session_id")?.asString()

        val path = call.request.path()
        val action = buildAuditAction(method, path)
        val targetType = extractTargetType(path)
        val targetId = extractTargetId(path)
        val requestIp = call.request.local.remoteHost
        val userAgent = call.request.headers["User-Agent"] ?: ""

        val payloadJson = Json.encodeToString(
            mapOf(
                "path" to path,
                "method" to method,
                "request_ip" to requestIp,
                "user_agent" to userAgent,
            )
        )

        // If impersonation JWT, actor is the SA (impersonated_by), impersonated_as is the sub
        val realActorId = impersonatedBy ?: actorId
        val impersonatedAsId = if (impersonatedBy != null) actorId else null

        try {
            newSuspendedTransaction {
                AuditLogTable.insert {
                    it[AuditLogTable.actorId] = UUID.fromString(realActorId)
                    it[AuditLogTable.action] = action
                    it[AuditLogTable.targetType] = targetType
                    it[AuditLogTable.targetId] = targetId?.let { tid -> UUID.fromString(tid) }
                    it[AuditLogTable.payload] = payloadJson
                    it[AuditLogTable.impersonatedAs] = impersonatedAsId?.let { aid -> UUID.fromString(aid) }
                    it[AuditLogTable.impersonationSessionId] = sessionId?.let { sid -> UUID.fromString(sid) }
                    it[AuditLogTable.createdAt] = OffsetDateTime.now(ZoneOffset.UTC)
                }
            }
        } catch (_: Exception) {
            // Audit failure must not break the request
        }
    }
}

private fun buildAuditAction(method: String, path: String): String {
    val segments = path.removePrefix("/sa/").split("/").filter { it.isNotBlank() }
    val resource = segments.firstOrNull() ?: "unknown"
    return when (method) {
        "POST" -> "$resource.create"
        "PATCH" -> "$resource.update"
        "DELETE" -> "$resource.delete"
        else -> "$resource.action"
    }
}

private fun extractTargetType(path: String): String? {
    val segments = path.removePrefix("/sa/").split("/").filter { it.isNotBlank() }
    return segments.firstOrNull()
}

private fun extractTargetId(path: String): String? {
    val segments = path.removePrefix("/sa/").split("/").filter { it.isNotBlank() }
    // Pattern: /sa/{resource}/{id}/...
    return segments.getOrNull(1)?.let { seg ->
        runCatching { UUID.fromString(seg); seg }.getOrNull()
    }
}
