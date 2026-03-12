package com.playbook.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import com.playbook.data.network.ApiConfig
import com.playbook.db.NotificationsQueries
import com.playbook.domain.Notification
import com.playbook.domain.PagedNotifications
import com.playbook.repository.NotificationRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotificationRepositoryImpl(
    private val client: HttpClient,
    private val config: ApiConfig,
    private val queries: NotificationsQueries,
) : NotificationRepository {

    override suspend fun getNotifications(page: Int, limit: Int): PagedNotifications {
        val paged: PagedNotifications = client.get("${config.baseUrl}/notifications") {
            bearerAuth(config.authTokenProvider() ?: "")
            parameter("page", page)
            parameter("limit", limit)
        }.body()
        paged.items.forEach { it.upsertLocal() }
        return paged
    }

    override suspend fun markRead(notificationId: String) {
        client.put("${config.baseUrl}/notifications/$notificationId/read") {
            bearerAuth(config.authTokenProvider() ?: "")
        }
        queries.markRead(id = notificationId)
    }

    override suspend fun markAllRead() {
        client.put("${config.baseUrl}/notifications/read-all") {
            bearerAuth(config.authTokenProvider() ?: "")
        }
        queries.markAllRead()
    }

    override suspend fun deleteNotification(notificationId: String) {
        client.delete("${config.baseUrl}/notifications/$notificationId") {
            bearerAuth(config.authTokenProvider() ?: "")
        }
        queries.delete(id = notificationId)
    }

    override fun getUnreadCount(): Flow<Int> =
        queries.getUnreadCount().asFlow().mapToOne(Dispatchers.Default).map { it.toInt() }

    override suspend fun syncFromServer() {
        val paged: PagedNotifications = client.get("${config.baseUrl}/notifications") {
            bearerAuth(config.authTokenProvider() ?: "")
            parameter("page", 0)
            parameter("limit", 20)
        }.body()
        paged.items.forEach { it.upsertLocal() }
    }

    private fun Notification.upsertLocal() {
        queries.upsert(
            id = id,
            userId = userId,
            type = type,
            title = title,
            body = body,
            deepLink = deepLink,
            referenceId = referenceId,
            read = if (read) 1L else 0L,
            createdAt = createdAt,
        )
    }
}
