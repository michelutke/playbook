package com.playbook.repository

import com.playbook.domain.NotificationSettings
import com.playbook.domain.UpdateNotificationSettingsRequest

interface NotificationSettingsRepository {
    suspend fun getSettings(): NotificationSettings
    suspend fun updateSettings(request: UpdateNotificationSettingsRequest): NotificationSettings
}
