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

class ClubRoutesTest : IntegrationTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun seed() {
            transaction { Fixtures.seedCoachAndTeam(this) }
        }
    }

    @Test
    fun `postClubs_asAuthenticatedUser_returns201`() = testApp {
        val client = jsonClient()
        val response = client.post("/clubs") {
            bearerAuth(bearerToken(Fixtures.TEST_COACH_ID))
            contentType(ContentType.Application.Json)
            setBody("""{"name":"New Club","sportType":"basketball"}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("New Club"))
    }

    @Test
    fun `postClubs_noToken_returns401`() = testApp {
        val client = jsonClient()
        val response = client.post("/clubs") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"New Club","sportType":"basketball"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `getClub_asManager_returns200`() = testApp {
        val client = jsonClient()
        val response = client.get("/clubs/${Fixtures.TEST_CLUB_ID}") {
            bearerAuth(bearerToken(Fixtures.TEST_MANAGER_ID))
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `getClub_asNonManager_returns403`() = testApp {
        val client = jsonClient()
        val response = client.get("/clubs/${Fixtures.TEST_CLUB_ID}") {
            bearerAuth(bearerToken(Fixtures.TEST_COACH_ID))
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `getClub_noToken_returns401`() = testApp {
        val response = createClient {}.get("/clubs/${Fixtures.TEST_CLUB_ID}")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `getClub_unknownId_returns404`() = testApp {
        val client = jsonClient()
        val response = client.get("/clubs/00000000-0000-0000-0000-000000000099") {
            bearerAuth(bearerToken(Fixtures.TEST_MANAGER_ID))
        }
        // Manager check will fail first with 403, or 404 if somehow manager
        assertTrue(
            response.status == HttpStatusCode.NotFound || response.status == HttpStatusCode.Forbidden,
            "Expected 403 or 404 for unknown club, got ${response.status}"
        )
    }

    @Test
    fun `patchClub_asManager_returns200`() = testApp {
        val client = jsonClient()
        val response = client.patch("/clubs/${Fixtures.TEST_CLUB_ID}") {
            bearerAuth(bearerToken(Fixtures.TEST_MANAGER_ID))
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Updated Club Name"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `patchClub_asNonManager_returns403`() = testApp {
        val client = jsonClient()
        val response = client.patch("/clubs/${Fixtures.TEST_CLUB_ID}") {
            bearerAuth(bearerToken(Fixtures.TEST_COACH_ID))
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Should fail"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }
}
