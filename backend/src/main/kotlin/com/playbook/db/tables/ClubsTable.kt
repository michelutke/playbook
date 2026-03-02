package com.playbook.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object ClubsTable : Table("clubs") {
    val id = uuid("id").autoGenerate()
    val name = text("name")
    val logoUrl = text("logo_url").nullable()
    val sportType = text("sport_type")
    val location = text("location").nullable()
    val status = text("status").default("active")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}
