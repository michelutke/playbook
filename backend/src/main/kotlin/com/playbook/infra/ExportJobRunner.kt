package com.playbook.infra

import com.playbook.db.tables.AuditLogTable
import com.playbook.db.tables.ExportJobsTable
import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.io.File
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * SA-032: Background coroutine that picks up pending export jobs,
 * runs CSV generation, and updates status.
 *
 * SA-034: Cleanup coroutine deletes export files and job rows older than 1 hour.
 */
fun Application.startExportJobRunner() {
    val scope = CoroutineScope(Dispatchers.IO)

    // SA-032: process pending jobs every 5 seconds
    scope.launch {
        while (isActive) {
            runCatching { processPendingExportJobs() }
                .onFailure { log.error("Export job runner failed", it) }
            delay(5_000L)
        }
    }

    // SA-034: cleanup every 15 minutes
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
            .map { row ->
                Triple(
                    row[ExportJobsTable.id].toString(),
                    row[ExportJobsTable.type],
                    row[ExportJobsTable.filters],
                )
            }
    }

    for ((jobId, type, filtersJson) in pendingJobs) {
        runCatching {
            // Mark running
            newSuspendedTransaction {
                ExportJobsTable.update({ ExportJobsTable.id eq UUID.fromString(jobId) }) {
                    it[status] = "running"
                }
            }

            val filePath = when (type) {
                "audit_log_csv" -> generateAuditLogCsv(jobId, filtersJson)
                else -> throw IllegalArgumentException("Unknown export type: $type")
            }

            // Mark done
            newSuspendedTransaction {
                ExportJobsTable.update({ ExportJobsTable.id eq UUID.fromString(jobId) }) {
                    it[status] = "done"
                    it[resultPath] = filePath
                    it[completedAt] = OffsetDateTime.now(ZoneOffset.UTC)
                }
            }
        }.onFailure { err ->
            newSuspendedTransaction {
                ExportJobsTable.update({ ExportJobsTable.id eq UUID.fromString(jobId) }) {
                    it[status] = "failed"
                    it[completedAt] = OffsetDateTime.now(ZoneOffset.UTC)
                }
            }
        }
    }
}

private suspend fun generateAuditLogCsv(jobId: String, filtersJson: String?): String {
    val rows = newSuspendedTransaction {
        AuditLogTable.selectAll()
            .orderBy(AuditLogTable.createdAt)
            .toList()
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

private suspend fun cleanupOldExports() {
    val cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1)

    val oldJobs = newSuspendedTransaction {
        ExportJobsTable.selectAll().where {
            (ExportJobsTable.createdAt less cutoff) and
            (ExportJobsTable.status eq "done")
        }.map { Pair(it[ExportJobsTable.id].toString(), it[ExportJobsTable.resultPath]) }
    }

    for ((jobId, path) in oldJobs) {
        // Delete file
        path?.let { File(it).delete() }
        // Delete row
        newSuspendedTransaction {
            ExportJobsTable.deleteWhere { id eq UUID.fromString(jobId) }
        }
    }
}
