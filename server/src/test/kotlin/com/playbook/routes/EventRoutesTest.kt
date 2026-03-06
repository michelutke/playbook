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

class EventRoutesTest : IntegrationTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun seed() {
            transaction { Fixtures.seedCoachAndTeam(this) }
        }
    }

    @Test
    fun `getMyEvents_asPlayer_returns200`() = testApp {
        val client = jsonClient()
        val response = client.get("/users/me/events") {
            bearerAuth(bearerToken(Fixtures.TEST_PLAYER_ID))
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `getMyEvents_noToken_returns401`() = testApp {
        val response = createClient {}.get("/users/me/events")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `getTeamEvents_asCoach_returns200`() = testApp {
        val client = jsonClient()
        val response = client.get("/teams/${Fixtures.TEST_TEAM_ID}/events") {
            bearerAuth(bearerToken(Fixtures.TEST_COACH_ID))
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `getTeamEvents_asNonCoach_returns403`() = testApp {
        val client = jsonClient()
        val response = client.get("/teams/${Fixtures.TEST_TEAM_ID}/events") {
            bearerAuth(bearerToken(Fixtures.TEST_PLAYER_ID))
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `getTeamEvents_noToken_returns401`() = testApp {
        val response = createClient {}.get("/teams/${Fixtures.TEST_TEAM_ID}/events")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `createEvent_asCoach_returns201`() = testApp {
        val client = jsonClient()
        val response = client.post("/events") {
            bearerAuth(bearerToken(Fixtures.TEST_COACH_ID))
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "teamIds":["${Fixtures.TEST_TEAM_ID}"],
                  "title":"Training Session",
                  "type":"TRAINING",
                  "startAt":"2026-06-01T10:00:00Z",
                  "endAt":"2026-06-01T12:00:00Z"
                }
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("Training Session"))
    }

    @Test
    fun `createEvent_asNonCoach_returns403`() = testApp {
        val client = jsonClient()
        val response = client.post("/events") {
            bearerAuth(bearerToken(Fixtures.TEST_PLAYER_ID))
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "teamIds":["${Fixtures.TEST_TEAM_ID}"],
                  "title":"Unauthorized",
                  "type":"TRAINING",
                  "startAt":"2026-06-01T10:00:00Z",
                  "endAt":"2026-06-01T12:00:00Z"
                }
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `createEvent_noToken_returns401`() = testApp {
        val client = jsonClient()
        val response = client.post("/events") {
            contentType(ContentType.Application.Json)
            setBody("""{"teamIds":["${Fixtures.TEST_TEAM_ID}"],"title":"x","type":"TRAINING","startAt":"2026-06-01T10:00:00Z","endAt":"2026-06-01T12:00:00Z"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `getEvent_unknownId_returns404`() = testApp {
        val client = jsonClient()
        val response = client.get("/events/00000000-0000-0000-0000-000000000099") {
            bearerAuth(bearerToken(Fixtures.TEST_COACH_ID))
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
