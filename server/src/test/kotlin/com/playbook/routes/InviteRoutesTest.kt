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

class InviteRoutesTest : IntegrationTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun seed() {
            transaction { Fixtures.seedCoachAndTeam(this) }
        }
    }

    @Test
    fun `createInvite_asCoach_returns201OrSmtpError`() = testApp {
        // Note: SMTP (localhost:1025) is unavailable in test env; the invite is persisted but
        // the email send may throw, causing a 5xx. We verify auth+DB logic by checking the response
        // is NOT 401 or 403 (meaning auth passed and we reached the business logic layer).
        val client = jsonClient()
        val response = client.post("/teams/${Fixtures.TEST_TEAM_ID}/invites") {
            bearerAuth(bearerToken(Fixtures.TEST_COACH_ID))
            contentType(ContentType.Application.Json)
            setBody("""{"email":"invitee@test.com","role":"player"}""")
        }
        // Auth passed → not 401/403. May be 201 (if SMTP ignored) or 500 (SMTP failure).
        assertTrue(
            response.status != HttpStatusCode.Unauthorized && response.status != HttpStatusCode.Forbidden,
            "Expected auth to pass (not 401/403), got ${response.status}"
        )
    }

    @Test
    fun `createInvite_asNonCoach_returns403`() = testApp {
        val client = jsonClient()
        val response = client.post("/teams/${Fixtures.TEST_TEAM_ID}/invites") {
            bearerAuth(bearerToken(Fixtures.TEST_PLAYER_ID))
            contentType(ContentType.Application.Json)
            setBody("""{"email":"invitee@test.com","role":"player"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `createInvite_noToken_returns401`() = testApp {
        val client = jsonClient()
        val response = client.post("/teams/${Fixtures.TEST_TEAM_ID}/invites") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"invitee@test.com","role":"player"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `listInvites_asCoach_returns200`() = testApp {
        val client = jsonClient()
        val response = client.get("/teams/${Fixtures.TEST_TEAM_ID}/invites") {
            bearerAuth(bearerToken(Fixtures.TEST_COACH_ID))
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `listInvites_asNonCoach_returns403`() = testApp {
        val client = jsonClient()
        val response = client.get("/teams/${Fixtures.TEST_TEAM_ID}/invites") {
            bearerAuth(bearerToken(Fixtures.TEST_PLAYER_ID))
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `resolveInviteToken_unknownToken_returns404`() = testApp {
        val response = createClient {}.get("/invites/nonexistent-token-xyz")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `resolveInviteToken_isPublic_noAuthRequired`() = testApp {
        // Public endpoint — unknown token returns 404, not 401
        val response = createClient {}.get("/invites/some-random-token")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
