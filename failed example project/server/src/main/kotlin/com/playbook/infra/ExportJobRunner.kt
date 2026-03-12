package com.playbook.infra

import com.playbook.db.tables.AuditLogTable
import com.playbook.db.tables.ExportJobsTable
import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.io.File
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * SA-032: Processes pending export jobs every 5 seconds.
 * SA-034: Cleanup coroutine deletes done/failed jobs + files older than 1 hour.
 */
fun Application.startExportJobRunner() {
    val scope = CoroutineScope(Dispatchers.IO)
    monitor.subscribe(ApplicationStopping) { scope.cancel() }

    scope.launch {
        while (isActive) {
            runCatching { processPendingExportJobs() }
                .onFailure { log.error("Export job runner failed", it) }
            delay(5_000L)
        }
    }

    scope.launch {
        while (isActive) {
            runCatching { cleanupOldExports() }
                .onFailure { log.error("Export cleanup failed", it) }
            delay(TimeUnit.MINUTES.toMillis(15))
        }
    }
}

private suspend fun processPendingExportJobs() {
    val pendingJobs = newSuspendedTransaction {
        ExportJobsTable.selectAll().where { ExportJobsTable.status eq "pending" }
            .map { Triple(it[ExportJobsTable.id].toString(), it[ExportJobsTable.type], it[ExportJobsTable.filters]) }
    }

    for ((jobId, type, filtersJson) in pendingJobs) {
        runCatching {
            newSuspendedTransaction {
                ExportJobsTable.update({ ExportJobsTable.id eq UUID.fromString(jobId) }) { it[status] = "running" }
            }
            val filePath = when (type) {
                "audit_log_csv" -> generateAuditLogCsv(jobId, filtersJson)
                else -> throw IllegalArgumentException("Unknown export type: $type")
            }
            newSuspendedTransaction {
                ExportJobsTable.update({ ExportJobsTable.id eq UUID.fromString(jobId) }) {
                    it[status] = "done"
                    it[resultPath] = filePath
                    it[completedAt] = OffsetDateTime.now(ZoneOffset.UTC)
                }
            }
        }.onFailure {
            newSuspendedTransaction {
                ExportJobsTable.update({ ExportJobsTable.id eq UUID.fromString(jobId) }) {
                    it[status] = "failed"
                    it[completedAt] = OffsetDateTime.now(ZoneOffset.UTC)
                }
            }
        }
    }
}

// H-1 fix: parse and apply filters from stored JSON
private suspend fun generateAuditLogCsv(jobId: String, filtersJson: String?): String {
    val filters = filtersJson?.let { json ->
        runCatching { Json.parseToJsonElement(json).jsonObject }.getOrNull()
    }

    val rows = newSuspendedTransaction {
        var query = AuditLogTable.selectAll()
        filters?.let { f ->
            f["actorId"]?.jsonPrimitive?.content?.let { id ->
                runCatching { UUID.fromString(id) }.getOrNull()?.let { uid ->
                    query = query.andWhere { AuditLogTable.actorId eq uid }
                }
            }
            f["action"]?.jsonPrimitive?.content?.let { a ->
                query = query.andWhere { AuditLogTable.action like "%$a%" }
            }
            f["from"]?.jsonPrimitive?.content?.let { from ->
                runCatching { OffsetDateTime.parse(from) }.getOrNull()?.let { dt ->
                    query = query.andWhere { AuditLogTable.createdAt greaterEq dt }
                }
            }
            f["to"]?.jsonPrimitive?.content?.let { to ->
                runCatching { OffsetDateTime.parse(to) }.getOrNull()?.let { dt ->
                    query = query.andWhere { AuditLogTable.createdAt less dt }
                }
            }
        }
        query.orderBy(AuditLogTable.createdAt).toList()
    }

    val exportDir = File(System.getProperty("user.dir"), "exports").apply { mkdirs() }
    val file = File(exportDir, "$jobId.csv")
    file.bufferedWriter().use { writer ->
        writer.write("id,actor_id,action,target_type,target_id,impersonated_as,created_at\n")
        for (row in rows) {
            writer.write(
                "${row[AuditLogTable.id]}," +
                "${row[AuditLogTable.actorId]}," +
                "\"${row[AuditLogTable.action].replace("\"", "\"\"")}\"," +
                "${row[AuditLogTable.targetType] ?: ""}," +
                "${row[AuditLogTable.targetId] ?: ""}," +
                "${row[AuditLogTable.impersonatedAs] ?: ""}," +
                "${row[AuditLogTable.createdAt]}\n"
            )
        }
    }
    return file.absolutePath
}

// M-3 fix: cleanup both done AND failed jobs older than 1h
private suspend fun cleanupOldExports() {
    val cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1)
    val oldJobs = newSuspendedTransaction {
        ExportJobsTable.selectAll().where {
            (ExportJobsTable.createdAt less cutoff) and
            (ExportJobsTable.status inList listOf("done", "failed"))
        }.map { Pair(it[ExportJobsTable.id].toString(), it[ExportJobsTable.resultPath]) }
    }
    for ((jobId, path) in oldJobs) {
        path?.let { File(it).delete() }
        newSuspendedTransaction {
            ExportJobsTable.deleteWhere { id eq UUID.fromString(jobId) }
        }
    }
}
