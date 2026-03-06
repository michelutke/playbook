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

class AbwesenheitRoutesTest : IntegrationTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun seed() {
            transaction { Fixtures.seedCoachAndTeam(this) }
        }
    }

    @Test
    fun `listAbwesenheit_asPlayer_returns200`() = testApp {
        val client = jsonClient()
        val response = client.get("/users/me/abwesenheit") {
            bearerAuth(bearerToken(Fixtures.TEST_PLAYER_ID))
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `listAbwesenheit_noToken_returns401`() = testApp {
        val response = createClient {}.get("/users/me/abwesenheit")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `createAbwesenheit_asPlayer_returns201`() = testApp {
        val client = jsonClient()
        val response = client.post("/users/me/abwesenheit") {
            bearerAuth(bearerToken(Fixtures.TEST_PLAYER_ID))
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "presetType": "WORK",
                  "label": "Mon work schedule",
                  "ruleType": "RECURRING",
                  "weekdays": [1, 2, 3, 4, 5]
                }
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `createAbwesenheit_noToken_returns401`() = testApp {
        val client = jsonClient()
        val response = client.post("/users/me/abwesenheit") {
            contentType(ContentType.Application.Json)
            setBody("""{"dayOfWeek":1,"startTime":"09:00","endTime":"17:00"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `getBackfillStatus_asPlayer_returns200`() = testApp {
        val client = jsonClient()
        val response = client.get("/users/me/abwesenheit/backfill-status") {
            bearerAuth(bearerToken(Fixtures.TEST_PLAYER_ID))
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `getBackfillStatus_noToken_returns401`() = testApp {
        val response = createClient {}.get("/users/me/abwesenheit/backfill-status")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `deleteAbwesenheit_unknownRule_returns404OrNoContent`() = testApp {
        val client = jsonClient()
        val response = client.delete("/users/me/abwesenheit/00000000-0000-0000-0000-000000000099") {
            bearerAuth(bearerToken(Fixtures.TEST_PLAYER_ID))
        }
        assertTrue(
            response.status == HttpStatusCode.NoContent || response.status == HttpStatusCode.NotFound,
            "Expected 204 or 404, got ${response.status}"
        )
    }
}
