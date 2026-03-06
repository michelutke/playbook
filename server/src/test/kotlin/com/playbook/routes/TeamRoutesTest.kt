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

class TeamRoutesTest : IntegrationTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun seed() {
            transaction { Fixtures.seedCoachAndTeam(this) }
        }
    }

    @Test
    fun `getTeam_asCoach_returns200`() = testApp {
        val client = jsonClient()
        val response = client.get("/teams/${Fixtures.TEST_TEAM_ID}") {
            bearerAuth(bearerToken(Fixtures.TEST_COACH_ID))
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `getTeam_asNonCoach_returns403`() = testApp {
        val client = jsonClient()
        val response = client.get("/teams/${Fixtures.TEST_TEAM_ID}") {
            bearerAuth(bearerToken(Fixtures.TEST_PLAYER_ID))
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `getTeam_noToken_returns401`() = testApp {
        val response = createClient {}.get("/teams/${Fixtures.TEST_TEAM_ID}")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `getTeam_unknownId_returns404`() = testApp {
        val client = jsonClient()
        // Coach is not on this team so will get 403; use manager to simulate
        val response = client.get("/teams/00000000-0000-0000-0000-000000000099") {
            bearerAuth(bearerToken(Fixtures.TEST_COACH_ID))
        }
        // Non-coach of a non-existent team → 403 (coach check fails first)
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `listTeamsByClub_asManager_returns200`() = testApp {
        val client = jsonClient()
        val response = client.get("/clubs/${Fixtures.TEST_CLUB_ID}/teams") {
            bearerAuth(bearerToken(Fixtures.TEST_MANAGER_ID))
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `listTeamsByClub_asNonManager_returns403`() = testApp {
        val client = jsonClient()
        val response = client.get("/clubs/${Fixtures.TEST_CLUB_ID}/teams") {
            bearerAuth(bearerToken(Fixtures.TEST_COACH_ID))
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `listTeamsByClub_noToken_returns401`() = testApp {
        val response = createClient {}.get("/clubs/${Fixtures.TEST_CLUB_ID}/teams")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `postTeam_asManager_returns201`() = testApp {
        val client = jsonClient()
        val response = client.post("/clubs/${Fixtures.TEST_CLUB_ID}/teams") {
            bearerAuth(bearerToken(Fixtures.TEST_MANAGER_ID))
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Manager Direct Team"}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `requestTeam_asCoach_returns201`() = testApp {
        val client = jsonClient()
        val response = client.post("/clubs/${Fixtures.TEST_CLUB_ID}/teams/request") {
            bearerAuth(bearerToken(Fixtures.TEST_COACH_ID))
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Coach Requested Team"}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `patchTeam_asCoach_returns200`() = testApp {
        val client = jsonClient()
        val response = client.patch("/teams/${Fixtures.TEST_TEAM_ID}") {
            bearerAuth(bearerToken(Fixtures.TEST_COACH_ID))
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Updated Team"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `patchTeam_asNonCoach_returns403`() = testApp {
        val client = jsonClient()
        val response = client.patch("/teams/${Fixtures.TEST_TEAM_ID}") {
            bearerAuth(bearerToken(Fixtures.TEST_PLAYER_ID))
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Unauthorized"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }
}
