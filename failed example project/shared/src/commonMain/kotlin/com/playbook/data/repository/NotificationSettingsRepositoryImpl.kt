package com.playbook.data.repository

import com.playbook.data.network.ApiConfig
import com.playbook.domain.NotificationSettings
import com.playbook.domain.UpdateNotificationSettingsRequest
import com.playbook.repository.NotificationSettingsRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class NotificationSettingsRepositoryImpl(
    private val client: HttpClient,
    private val config: ApiConfig,
) : NotificationSettingsRepository {

    override suspend fun getSettings(): NotificationSettings =
        client.get("${config.baseUrl}/notifications/settings") {
            bearerAuth(config.authTokenProvider() ?: "")
        }.body()

    override suspend fun updateSettings(request: UpdateNotificationSettingsRequest): NotificationSettings =
        client.put("${config.baseUrl}/notifications/settings") {
            bearerAuth(config.authTokenProvider() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
