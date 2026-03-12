package com.playbook.repository

import com.playbook.domain.PagedNotifications
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    suspend fun getNotifications(page: Int = 0, limit: Int = 20): PagedNotifications
    suspend fun markRead(notificationId: String)
    suspend fun markAllRead()
    suspend fun deleteNotification(notificationId: String)
    fun getUnreadCount(): Flow<Int>
    suspend fun syncFromServer()
}
