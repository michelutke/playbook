package com.playbook.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object AbwesenheitBackfillJobsTable : Table("abwesenheit_backfill_jobs") {
    val id = uuid("id").autoGenerate()
    val ruleId = uuid("rule_id").references(AbwesenheitRulesTable.id, onDelete = ReferenceOption.CASCADE)
    val status = text("status").default("pending")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}
