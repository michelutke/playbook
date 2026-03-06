package com.playbook.push

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class OneSignalPushService : PushService {

    private val log = LoggerFactory.getLogger(OneSignalPushService::class.java)

    private val appId: String = System.getenv("ONESIGNAL_APP_ID") ?: ""
    private val restApiKey: String = System.getenv("ONESIGNAL_REST_API_KEY") ?: ""

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    override suspend fun send(
        userIds: List<String>,
        title: String,
        body: String,
        deepLink: String,
        data: Map<String, String>,
    ) {
        if (appId.isBlank() || restApiKey.isBlank()) {
            log.warn("OneSignal not configured — skipping push for ${userIds.size} users")
            return
        }
        if (userIds.isEmpty()) return

        userIds.chunked(BATCH_SIZE).forEach { batch ->
            sendBatch(batch, title, body, deepLink, data)
        }
    }

    private suspend fun sendBatch(
        userIds: List<String>,
        title: String,
        body: String,
        deepLink: String,
        data: Map<String, String>,
    ) {
        val payload = OneSignalPayload(
            app_id = appId,
            include_aliases = IncludeAliases(external_id = userIds),
            target_channel = "push",
            headings = mapOf("en" to title),
            contents = mapOf("en" to body),
            url = deepLink,
            data = data.ifEmpty { null },
        )

        runCatching {
            client.post("https://api.onesignal.com/notifications") {
                header(HttpHeaders.Authorization, "Basic $restApiKey")
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
        }.onFailure { ex ->
            log.error("OneSignal push failed for batch of ${userIds.size} users", ex)
        }
    }

    @Serializable
    private data class OneSignalPayload(
        val app_id: String,
        val include_aliases: IncludeAliases,
        val target_channel: String,
        val headings: Map<String, String>,
        val contents: Map<String, String>,
        val url: String,
        val data: Map<String, String>? = null,
    )

    @Serializable
    private data class IncludeAliases(
        val external_id: List<String>,
    )

    companion object {
        private const val BATCH_SIZE = 2000
    }
}
