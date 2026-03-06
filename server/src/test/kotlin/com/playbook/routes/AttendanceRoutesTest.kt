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

class AttendanceRoutesTest : IntegrationTestBase() {

    companion object {
        // Reuse an event created in setup; in full integration we'd seed it here
        @BeforeAll
        @JvmStatic
        fun seed() {
            transaction { Fixtures.seedCoachAndTeam(this) }
        }
    }

    @Test
    fun `getAttendanceMe_noToken_returns401`() = testApp {
        val response = createClient {}.get("/events/00000000-0000-0000-0000-000000000001/attendance/me")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `getTeamAttendance_asCoach_returns200ForKnownTeam`() = testApp {
        val client = jsonClient()
        val response = client.get("/teams/${Fixtures.TEST_TEAM_ID}/attendance") {
            bearerAuth(bearerToken(Fixtures.TEST_COACH_ID))
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `getTeamAttendance_asNonCoach_returns403`() = testApp {
        val client = jsonClient()
        val response = client.get("/teams/${Fixtures.TEST_TEAM_ID}/attendance") {
            bearerAuth(bearerToken(Fixtures.TEST_PLAYER_ID))
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `getTeamAttendance_noToken_returns401`() = testApp {
        val response = createClient {}.get("/teams/${Fixtures.TEST_TEAM_ID}/attendance")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `getUserAttendance_asSelf_returns200`() = testApp {
        val client = jsonClient()
        val response = client.get("/users/${Fixtures.TEST_PLAYER_ID}/attendance") {
            bearerAuth(bearerToken(Fixtures.TEST_PLAYER_ID))
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `getUserAttendance_asCoachOnSameTeam_returns200`() = testApp {
        val client = jsonClient()
        val response = client.get("/users/${Fixtures.TEST_PLAYER_ID}/attendance") {
            bearerAuth(bearerToken(Fixtures.TEST_COACH_ID))
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `getUserAttendance_noToken_returns401`() = testApp {
        val response = createClient {}.get("/users/${Fixtures.TEST_PLAYER_ID}/attendance")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
