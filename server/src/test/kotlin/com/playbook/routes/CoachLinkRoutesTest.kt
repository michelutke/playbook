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

class CoachLinkRoutesTest : IntegrationTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun seed() {
            transaction { Fixtures.seedCoachAndTeam(this) }
        }
    }

    @Test
    fun `getCoachLink_asManager_returnsLinkOrNotFound`() = testApp {
        val client = jsonClient()
        val response = client.get("/clubs/${Fixtures.TEST_CLUB_ID}/coach-link") {
            bearerAuth(bearerToken(Fixtures.TEST_MANAGER_ID))
        }
        // 404 if no link created yet, 200 if exists
        assertTrue(
            response.status == HttpStatusCode.OK || response.status == HttpStatusCode.NotFound,
            "Expected 200 or 404, got ${response.status}"
        )
    }

    @Test
    fun `getCoachLink_asNonManager_returns403`() = testApp {
        val client = jsonClient()
        val response = client.get("/clubs/${Fixtures.TEST_CLUB_ID}/coach-link") {
            bearerAuth(bearerToken(Fixtures.TEST_COACH_ID))
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `getCoachLink_noToken_returns401`() = testApp {
        val response = createClient {}.get("/clubs/${Fixtures.TEST_CLUB_ID}/coach-link")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `rotateCoachLink_asManager_returns201`() = testApp {
        val client = jsonClient()
        val response = client.post("/clubs/${Fixtures.TEST_CLUB_ID}/coach-link") {
            bearerAuth(bearerToken(Fixtures.TEST_MANAGER_ID))
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `rotateCoachLink_asNonManager_returns403`() = testApp {
        val client = jsonClient()
        val response = client.post("/clubs/${Fixtures.TEST_CLUB_ID}/coach-link") {
            bearerAuth(bearerToken(Fixtures.TEST_COACH_ID))
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `resolveCoachLink_unknownToken_returns404`() = testApp {
        val response = createClient {}.get("/club-links/nonexistent-token")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `resolveCoachLink_isPublic_noAuthRequired`() = testApp {
        // Public endpoint — unknown token returns 404, not 401
        val response = createClient {}.get("/club-links/some-token")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `deleteCoachLink_asManager_returns204`() = testApp {
        // First rotate to ensure a link exists
        val client = jsonClient()
        client.post("/clubs/${Fixtures.TEST_CLUB_ID}/coach-link") {
            bearerAuth(bearerToken(Fixtures.TEST_MANAGER_ID))
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        val response = client.delete("/clubs/${Fixtures.TEST_CLUB_ID}/coach-link") {
            bearerAuth(bearerToken(Fixtures.TEST_MANAGER_ID))
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
    }
}
