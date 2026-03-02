package com.playbook.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

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
