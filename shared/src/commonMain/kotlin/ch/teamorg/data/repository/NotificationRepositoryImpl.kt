package ch.teamorg.data.repository

import ch.teamorg.data.NotificationCacheManager
import ch.teamorg.domain.MarkAllReadResponse
import ch.teamorg.domain.Notification
import ch.teamorg.domain.NotificationSettings
import ch.teamorg.domain.ReminderOverride
import ch.teamorg.domain.UnreadCountResponse
import ch.teamorg.domain.UpdateNotificationSettingsRequest
import ch.teamorg.repository.NotificationRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.errors.IOException

class NotificationRepositoryImpl(
    private val httpClient: HttpClient,
    private val cacheManager: NotificationCacheManager
) : NotificationRepository {

    override suspend fun getNotifications(limit: Int, offset: Int): Result<List<Notification>> {
        return try {
            val notifications: List<Notification> = httpClient.get("/notifications") {
                parameter("limit", limit)
                parameter("offset", offset)
            }.body()
            cacheManager.saveNotifications(notifications)
            Result.success(notifications)
        } catch (e: ConnectTimeoutException) {
            Result.success(cacheManager.getCachedNotifications(limit.toLong(), offset.toLong()))
        } catch (e: HttpRequestTimeoutException) {
            Result.success(cacheManager.getCachedNotifications(limit.toLong(), offset.toLong()))
        } catch (e: IOException) {
            Result.success(cacheManager.getCachedNotifications(limit.toLong(), offset.toLong()))
        }
    }

    override suspend fun getUnreadCount(): Result<Long> {
        return try {
            val response: UnreadCountResponse = httpClient.get("/notifications/unread-count").body()
            Result.success(response.count)
        } catch (e: ConnectTimeoutException) {
            Result.success(cacheManager.getUnreadCount())
        } catch (e: HttpRequestTimeoutException) {
            Result.success(cacheManager.getUnreadCount())
        } catch (e: IOException) {
            Result.success(cacheManager.getUnreadCount())
        }
    }

    override suspend fun markRead(notificationId: String): Result<Boolean> {
        return try {
            httpClient.post("/notifications/$notificationId/read")
            cacheManager.markRead(notificationId)
            Result.success(true)
        } catch (e: ConnectTimeoutException) {
            Result.failure(Exception("Offline — cannot mark notification as read"))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Offline — cannot mark notification as read"))
        } catch (e: IOException) {
            Result.failure(Exception("Offline — cannot mark notification as read"))
        }
    }

    override suspend fun markAllRead(): Result<Int> {
        return try {
            val response: MarkAllReadResponse = httpClient.post("/notifications/read-all").body()
            cacheManager.markAllRead()
            Result.success(response.marked)
        } catch (e: ConnectTimeoutException) {
            Result.failure(Exception("Offline — cannot mark all read"))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Offline — cannot mark all read"))
        } catch (e: IOException) {
            Result.failure(Exception("Offline — cannot mark all read"))
        }
    }

    override suspend fun getSettings(teamId: String): Result<NotificationSettings> {
        return try {
            Result.success(httpClient.get("/notifications/settings/$teamId").body())
        } catch (e: ConnectTimeoutException) {
            Result.failure(Exception("Offline — cannot load notification settings"))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Offline — cannot load notification settings"))
        } catch (e: IOException) {
            Result.failure(Exception("Offline — cannot load notification settings"))
        }
    }

    override suspend fun updateSettings(
        teamId: String,
        request: UpdateNotificationSettingsRequest
    ): Result<Unit> {
        return try {
            httpClient.put("/notifications/settings/$teamId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(Unit)
        } catch (e: ConnectTimeoutException) {
            Result.failure(Exception("Offline — cannot update notification settings"))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Offline — cannot update notification settings"))
        } catch (e: IOException) {
            Result.failure(Exception("Offline — cannot update notification settings"))
        }
    }

    override suspend fun getReminderOverride(eventId: String): Result<ReminderOverride> {
        return try {
            Result.success(httpClient.get("/events/$eventId/reminder").body())
        } catch (e: ConnectTimeoutException) {
            Result.failure(Exception("Offline — cannot load reminder override"))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Offline — cannot load reminder override"))
        } catch (e: IOException) {
            Result.failure(Exception("Offline — cannot load reminder override"))
        }
    }

    override suspend fun setReminderOverride(eventId: String, leadMinutes: Int?): Result<Unit> {
        return try {
            httpClient.put("/events/$eventId/reminder") {
                contentType(ContentType.Application.Json)
                setBody(ReminderOverride(leadMinutes))
            }
            Result.success(Unit)
        } catch (e: ConnectTimeoutException) {
            Result.failure(Exception("Offline — cannot set reminder override"))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Offline — cannot set reminder override"))
        } catch (e: IOException) {
            Result.failure(Exception("Offline — cannot set reminder override"))
        }
    }
}
