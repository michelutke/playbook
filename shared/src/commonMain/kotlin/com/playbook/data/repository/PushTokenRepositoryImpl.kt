package com.playbook.data.repository

import com.playbook.data.network.ApiConfig
import com.playbook.domain.PushToken
import com.playbook.repository.PushTokenRepository
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

class PushTokenRepositoryImpl(
    private val client: HttpClient,
    private val config: ApiConfig,
) : PushTokenRepository {

    override suspend fun registerToken(platform: String, token: String) {
        client.post("${config.baseUrl}/push-tokens") {
            bearerAuth(config.authTokenProvider() ?: "")
            contentType(ContentType.Application.Json)
            setBody(PushToken(platform = platform, token = token))
        }
    }

    override suspend fun deregisterToken(token: String) {
        client.delete("${config.baseUrl}/push-tokens/$token") {
            bearerAuth(config.authTokenProvider() ?: "")
        }
    }
}
