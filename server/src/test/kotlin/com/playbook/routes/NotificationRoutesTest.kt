package com.playbook.routes

import com.playbook.test.Fixtures
import com.playbook.test.IntegrationTestBase
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationRoutesTest : IntegrationTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun seed() {
            transaction { Fixtures.seedCoachAndTeam(this) }
        }
    }

    @Test
    fun `getNotifications_asPlayer_returns200`() = testApp {
        val client = jsonClient()
        val response = client.get("/notifications") {
            bearerAuth(bearerToken(Fixtures.TEST_PLAYER_ID))
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `getNotifications_noToken_returns401`() = testApp {
        val response = createClient {}.get("/notifications")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `markAllRead_asPlayer_returns204`() = testApp {
        val client = jsonClient()
        val response = client.put("/notifications/read-all") {
            bearerAuth(bearerToken(Fixtures.TEST_PLAYER_ID))
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `markAllRead_noToken_returns401`() = testApp {
        val response = createClient {}.put("/notifications/read-all")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `getNotificationSettings_asPlayer_returns200`() = testApp {
        val client = jsonClient()
        val response = client.get("/notifications/settings") {
            bearerAuth(bearerToken(Fixtures.TEST_PLAYER_ID))
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `getNotificationSettings_noToken_returns401`() = testApp {
        val response = createClient {}.get("/notifications/settings")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `registerPushToken_asPlayer_returns204`() = testApp {
        val client = jsonClient()
        val response = client.post("/push-tokens") {
            bearerAuth(bearerToken(Fixtures.TEST_PLAYER_ID))
            contentType(ContentType.Application.Json)
            setBody("""{"platform":"android","token":"fake-push-token-abc123"}""")
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `registerPushToken_noToken_returns401`() = testApp {
        val client = jsonClient()
        val response = client.post("/push-tokens") {
            contentType(ContentType.Application.Json)
            setBody("""{"platform":"android","token":"abc"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `deleteNotification_unknownId_returns204OrNotFound`() = testApp {
        val client = jsonClient()
        val response = client.delete("/notifications/00000000-0000-0000-0000-000000000099") {
            bearerAuth(bearerToken(Fixtures.TEST_PLAYER_ID))
        }
        assertTrue(
            response.status == HttpStatusCode.NoContent || response.status == HttpStatusCode.NotFound,
            "Expected 204 or 404, got ${response.status}"
        )
    }
}
