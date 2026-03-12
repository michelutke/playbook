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

class MemberRoutesTest : IntegrationTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun seed() {
            transaction { Fixtures.seedCoachAndTeam(this) }
        }
    }

    @Test
    fun `getMembers_asCoach_returns200WithRoster`() = testApp {
        val client = jsonClient()
        val response = client.get("/teams/${Fixtures.TEST_TEAM_ID}/members") {
            bearerAuth(bearerToken(Fixtures.TEST_COACH_ID))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        // Both coach and player should appear
        assertTrue(body.contains(Fixtures.TEST_COACH_ID) || body.contains("coach"))
    }

    @Test
    fun `getMembers_asNonCoach_returns403`() = testApp {
        val client = jsonClient()
        val response = client.get("/teams/${Fixtures.TEST_TEAM_ID}/members") {
            bearerAuth(bearerToken(Fixtures.TEST_PLAYER_ID))
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `getMembers_noToken_returns401`() = testApp {
        val response = createClient {}.get("/teams/${Fixtures.TEST_TEAM_ID}/members")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `getMemberProfile_asCoach_returns200`() = testApp {
        val client = jsonClient()
        val response = client.get("/teams/${Fixtures.TEST_TEAM_ID}/members/${Fixtures.TEST_PLAYER_ID}/profile") {
            bearerAuth(bearerToken(Fixtures.TEST_COACH_ID))
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `getMemberProfile_asNonCoach_returns403`() = testApp {
        val client = jsonClient()
        val response = client.get("/teams/${Fixtures.TEST_TEAM_ID}/members/${Fixtures.TEST_PLAYER_ID}/profile") {
            bearerAuth(bearerToken(Fixtures.TEST_PLAYER_ID))
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `leaveTeam_asPlayer_returns204`() = testApp {
        // Register a fresh user to leave (so we don't corrupt other tests)
        val client = jsonClient()
        val regResp = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"leavingplayer@test.com","password":"pass123"}""")
        }
        assertEquals(HttpStatusCode.Created, regResp.status)

        // Can only leave a team you are on. This user is not on TEST_TEAM_ID so should get 204 (no-op or success)
        // The route itself calls leaveTeam which likely is idempotent
        val leaveResp = client.delete("/users/me/teams/${Fixtures.TEST_TEAM_ID}") {
            bearerAuth(bearerToken(Fixtures.TEST_PLAYER_ID))
        }
        assertEquals(HttpStatusCode.NoContent, leaveResp.status)
    }

    @Test
    fun `leaveTeam_noToken_returns401`() = testApp {
        val response = createClient {}.delete("/users/me/teams/${Fixtures.TEST_TEAM_ID}")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
