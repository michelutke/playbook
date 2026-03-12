package com.playbook

import com.playbook.test.Fixtures
import com.playbook.test.IntegrationTestBase
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Security regression tests.
 *
 * OWASP A01: Authorization checked per route via authMiddleware (requireClubManager, requireCoachOnTeam etc.)
 * OWASP A07: JWT validated by Ktor auth plugin before route handler runs.
 */
class SecurityTest : IntegrationTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun seed() {
            transaction { Fixtures.seedCoachAndTeam(this) }
        }
    }

    @Test
    fun `unauthenticated_GET_teams_returns401`() = testApp {
        val response = createClient {}.get("/teams/${Fixtures.TEST_TEAM_ID}")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `unauthenticated_POST_clubs_returns401`() = testApp {
        val client = jsonClient()
        val response = client.post("/clubs") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"X","sportType":"football"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `regularUserToken_on_SA_endpoint_returns401`() = testApp {
        // Regular "playbook-app" audience token fails the "super-admin" JWT verifier → 401, not 403
        val client = jsonClient()
        val response = client.get("/sa/clubs") {
            bearerAuth(bearerToken(Fixtures.TEST_COACH_ID))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `expiredToken_returns401`() = testApp {
        val client = jsonClient()
        val response = client.get("/teams/${Fixtures.TEST_TEAM_ID}") {
            bearerAuth(expiredToken(Fixtures.TEST_COACH_ID))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `tamperedToken_returns401`() = testApp {
        val client = jsonClient()
        val token = bearerToken(Fixtures.TEST_COACH_ID)
        // Tamper the first char of the signature segment (encodes real bits, not base64 padding)
        val parts = token.split(".")
        val sig = parts[2]
        val tamperedSig = (if (sig[0] == 'A') 'B' else 'A') + sig.drop(1)
        val tampered = "${parts[0]}.${parts[1]}.$tamperedSig"
        val response = client.get("/teams/${Fixtures.TEST_TEAM_ID}") {
            bearerAuth(tampered)
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `malformedJson_POST_teams_returns400`() = testApp {
        val client = jsonClient()
        val response = client.post("/clubs/${Fixtures.TEST_CLUB_ID}/teams") {
            bearerAuth(bearerToken(Fixtures.TEST_MANAGER_ID))
            contentType(ContentType.Application.Json)
            setBody("{ this is not json }")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `sqlInjection_inQueryParam_doesNotReturn500`() = testApp {
        val client = jsonClient()
        // SQL injection attempt in status filter query param
        val response = client.get("/clubs/${Fixtures.TEST_CLUB_ID}/teams?status='; DROP TABLE users;--") {
            bearerAuth(bearerToken(Fixtures.TEST_MANAGER_ID))
        }
        // Must NOT return 500; any 2xx/4xx is acceptable (parameterized queries protect us)
        assertNotEquals(HttpStatusCode.InternalServerError, response.status)
    }

    @Test
    fun `wrongAudienceToken_onRegularEndpoint_returns401`() = testApp {
        // SA audience token should not work on regular protected endpoints
        val client = jsonClient()
        val response = client.get("/users/me") {
            bearerAuth(saToken(Fixtures.TEST_SA_ID))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
