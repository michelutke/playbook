package ch.teamorg.infra

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.slf4j.LoggerFactory

interface PushService {
    suspend fun sendToUsers(
        userIds: List<String>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    )
}

private val logger = LoggerFactory.getLogger("PushService")

class PushServiceImpl(
    private val client: HttpClient,
    private val appId: String,
    private val apiKey: String
) : PushService {
    override suspend fun sendToUsers(
        userIds: List<String>,
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        if (userIds.isEmpty() || appId.isBlank() || apiKey.isBlank()) return
        try {
            val payload = buildJsonObject {
                put("app_id", appId)
                putJsonObject("include_aliases") {
                    putJsonArray("external_id") {
                        userIds.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
                    }
                }
                put("target_channel", "push")
                putJsonObject("contents") { put("en", body) }
                putJsonObject("headings") { put("en", title) }
                putJsonObject("data") {
                    data.forEach { (k, v) -> put(k, v) }
                }
            }
            client.post("https://api.onesignal.com/notifications?c=push") {
                header("Authorization", "Key $apiKey")
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
        } catch (e: Exception) {
            logger.error("PushService: failed to send push to ${userIds.size} users", e)
        }
    }
}
