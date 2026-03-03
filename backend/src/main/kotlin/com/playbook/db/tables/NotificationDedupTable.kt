package com.playbook.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object NotificationDedupTable : Table("notification_dedup") {
    val key = text("key")
    val createdAt = timestampWithTimeZone("created_at")
    override val primaryKey = PrimaryKey(key)
}
