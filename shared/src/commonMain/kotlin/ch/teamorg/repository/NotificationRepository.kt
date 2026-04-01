package ch.teamorg.repository

import ch.teamorg.domain.Notification
import ch.teamorg.domain.NotificationSettings
import ch.teamorg.domain.ReminderOverride
import ch.teamorg.domain.UpdateNotificationSettingsRequest

interface NotificationRepository {
    suspend fun getNotifications(limit: Int = 50, offset: Int = 0): Result<List<Notification>>
    suspend fun getUnreadCount(): Result<Long>
    suspend fun markRead(notificationId: String): Result<Boolean>
    suspend fun markAllRead(): Result<Int>
    suspend fun deleteAll(): Result<Unit>
    suspend fun getSettings(teamId: String): Result<NotificationSettings>
    suspend fun updateSettings(teamId: String, request: UpdateNotificationSettingsRequest): Result<Unit>
    suspend fun getReminderOverride(eventId: String): Result<ReminderOverride>
    suspend fun setReminderOverride(eventId: String, leadMinutes: Int?): Result<Unit>
}
