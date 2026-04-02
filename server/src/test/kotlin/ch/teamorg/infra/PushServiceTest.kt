package ch.teamorg.infra

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PushServiceTest {

    @Test
    fun `sendToUsers calls OneSignal REST API with correct payload`() {
        var capturedUrl: String? = null
        var capturedBody: String? = null

        val mockEngine = MockEngine { request ->
            capturedUrl = request.url.toString()
            capturedBody = (request.body as? TextContent)?.text
            respond(
                content = """{"id":"abc"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
        val pushService = PushServiceImpl(client, appId = "test-app-id", apiKey = "test-api-key")

        runBlocking {
            pushService.sendToUsers(
                userIds = listOf("user-1", "user-2"),
                title = "Test Title",
                body = "Test Body",
                data = mapOf("entity_id" to "event-123")
            )
        }

        assertNotNull(capturedUrl, "MockEngine should have been called")
        assertEquals("https://api.onesignal.com/notifications?c=push", capturedUrl)

        val body = capturedBody
        assertNotNull(body, "Request body should not be null")
        assertTrue(body.contains("include_aliases"), "Body should contain include_aliases")
        assertTrue(body.contains("user-1"), "Body should contain user-1")
        assertTrue(body.contains("user-2"), "Body should contain user-2")
        assertTrue(body.contains("Test Title"), "Body should contain title")
        assertTrue(body.contains("Test Body"), "Body should contain body")
    }

    @Test
    fun `sendToUsers with empty list does not call API`() {
        var called = false

        val mockEngine = MockEngine {
            called = true
            respondOk()
        }

        val client = HttpClient(mockEngine)
        val pushService = PushServiceImpl(client, appId = "test-app-id", apiKey = "test-api-key")

        runBlocking {
            pushService.sendToUsers(emptyList(), "t", "b")
        }

        assertFalse(called, "API should not be called when userIds is empty")
    }

    @Test
    fun `sendToUsers handles API error gracefully`() {
        val mockEngine = MockEngine {
            respond("Internal Server Error", status = HttpStatusCode.InternalServerError)
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
        val pushService = PushServiceImpl(client, appId = "test-app-id", apiKey = "test-api-key")

        // Should not throw even on 500
        runBlocking {
            pushService.sendToUsers(listOf("user-1"), "t", "b")
        }
        // If we get here without exception, the test passes
    }
}
