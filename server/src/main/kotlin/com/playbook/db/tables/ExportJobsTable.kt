package com.playbook.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

/**
 * SA-028–034: Async CSV export jobs processed by [ExportJobRunner].
 *
 * Status transitions: pending → processing → done | failed
 * Jobs older than 1 hour (done or failed) are purged by the cleanup coroutine.
 *
 * Column notes:
 * - [type]        — currently only "audit_log_csv"
 * - [filters]     — JSON object with optional keys: actorId, action, from, to (ISO-8601 strings)
 * - [resultPath]  — absolute filesystem path to the generated CSV; null until done
 * - [completedAt] — set when status transitions to "done" or "failed"
 */
object ExportJobsTable : Table("export_jobs") {
    val id = uuid("id").autoGenerate()
    val type = text("type")
    val actorId = uuid("actor_id").references(UsersTable.id)
    val status = text("status").default("pending")
    val filters = text("filters").nullable()
    val resultPath = text("result_path").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val completedAt = timestampWithTimeZone("completed_at").nullable()
    override val primaryKey = PrimaryKey(id)
}
