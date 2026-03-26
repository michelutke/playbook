package ch.teamorg.infra

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
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

class PushServiceImpl(private val client: HttpClient) : PushService {
    override suspend fun sendToUsers(
        userIds: List<String>,
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        if (userIds.isEmpty()) return
        try {
            client.post("https://api.onesignal.com/notifications?c=push") {
                header("Authorization", "Key ${System.getenv("ONESIGNAL_API_KEY")}")
                contentType(ContentType.Application.Json)
                setBody(
                    mapOf(
                        "app_id" to System.getenv("ONESIGNAL_APP_ID"),
                        "include_aliases" to mapOf("external_id" to userIds),
                        "target_channel" to "push",
                        "contents" to mapOf("en" to body),
                        "headings" to mapOf("en" to title),
                        "data" to data
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("PushService: failed to send push to ${userIds.size} users", e)
        }
    }
}
