package ch.teamorg.data

import ch.teamorg.db.TeamorgDb
import ch.teamorg.domain.Notification
import kotlinx.datetime.Instant

class NotificationCacheManager(private val db: TeamorgDb) {
    private val queries get() = db.notificationQueries

    fun saveNotifications(notifications: List<Notification>) {
        notifications.forEach { n ->
            queries.upsertNotification(
                id = n.id,
                type = n.type,
                title = n.title,
                body = n.body,
                entity_id = n.entityId,
                entity_type = n.entityType,
                is_read = if (n.isRead) 1L else 0L,
                created_at = parseIsoToEpochMillis(n.createdAt)
            )
        }
    }

    fun getCachedNotifications(limit: Long, offset: Long): List<Notification> {
        return queries.getNotifications(limit, offset).executeAsList().map { row ->
            Notification(
                id = row.id,
                type = row.type,
                title = row.title,
                body = row.body,
                entityId = row.entity_id,
                entityType = row.entity_type,
                isRead = row.is_read == 1L,
                createdAt = epochMillisToIso(row.created_at)
            )
        }
    }

    fun getUnreadCount(): Long = queries.getUnreadCount().executeAsOne()

    fun markRead(id: String) { queries.markRead(id) }

    fun markAllRead() { queries.markAllRead() }

    fun cleanup(olderThanMillis: Long) { queries.deleteOlderThan(olderThanMillis) }

    private fun parseIsoToEpochMillis(iso: String): Long =
        Instant.parse(iso).toEpochMilliseconds()

    private fun epochMillisToIso(millis: Long): String =
        Instant.fromEpochMilliseconds(millis).toString()
}
