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

class SuperAdminRoutesTest : IntegrationTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun seed() {
            transaction { Fixtures.seedCoachAndTeam(this) }
        }
    }

    // SA token uses "playbook-sa" audience and requires superAdmin=true in DB
    private fun validSaToken() = saToken(Fixtures.TEST_SA_ID)

    @Test
    fun `getSaStats_withSaToken_returns200`() = testApp {
        val client = jsonClient()
        val response = client.get("/sa/stats") {
            bearerAuth(validSaToken())
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `getSaStats_withRegularToken_returns401`() = testApp {
        // Regular "playbook-app" audience token won't pass "super-admin" verifier
        val client = jsonClient()
        val response = client.get("/sa/stats") {
            bearerAuth(bearerToken(Fixtures.TEST_COACH_ID))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `getSaStats_noToken_returns401`() = testApp {
        val response = createClient {}.get("/sa/stats")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `listSaClubs_withSaToken_returns200`() = testApp {
        val client = jsonClient()
        val response = client.get("/sa/clubs") {
            bearerAuth(validSaToken())
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("Test Club"))
    }

    @Test
    fun `listSaClubs_withRegularToken_returns401`() = testApp {
        val client = jsonClient()
        val response = client.get("/sa/clubs") {
            bearerAuth(bearerToken(Fixtures.TEST_COACH_ID))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `getSaClub_withSaToken_returns200`() = testApp {
        val client = jsonClient()
        val response = client.get("/sa/clubs/${Fixtures.TEST_CLUB_ID}") {
            bearerAuth(validSaToken())
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `getSaClub_unknownId_returns404`() = testApp {
        val client = jsonClient()
        val response = client.get("/sa/clubs/00000000-0000-0000-0000-000000000099") {
            bearerAuth(validSaToken())
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `createSaClub_withSaToken_returns201`() = testApp {
        val client = jsonClient()
        val response = client.post("/sa/clubs") {
            bearerAuth(validSaToken())
            contentType(ContentType.Application.Json)
            setBody("""{"name":"SA Created Club","sportType":"volleyball"}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `getBillingSummary_withSaToken_returns200`() = testApp {
        val client = jsonClient()
        val response = client.get("/sa/billing/summary") {
            bearerAuth(validSaToken())
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `getAuditLog_withSaToken_returns200`() = testApp {
        val client = jsonClient()
        val response = client.get("/sa/audit-log") {
            bearerAuth(validSaToken())
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `searchUsers_withSaToken_returns200`() = testApp {
        val client = jsonClient()
        val response = client.get("/sa/users/search?q=coach") {
            bearerAuth(validSaToken())
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `searchUsers_missingQ_returns400`() = testApp {
        val client = jsonClient()
        val response = client.get("/sa/users/search") {
            bearerAuth(validSaToken())
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
