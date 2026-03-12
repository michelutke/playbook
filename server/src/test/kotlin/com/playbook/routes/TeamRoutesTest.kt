package com.playbook.routes

import com.playbook.domain.models.Club
import com.playbook.domain.models.Team
import com.playbook.domain.models.TeamMember
import com.playbook.test.IntegrationTestBase
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TeamRoutesTest : IntegrationTestBase() {

    @Test
    fun `create team success`() = withPlaybookTestApplication {
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val auth = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("teamcr@example.com", "password123", "Creator"))
        }.body<AuthResponse>()

        val clubId = client.post("/clubs") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateClubRequest("Team Club"))
        }.body<Club>().id

        val response = client.post("/clubs/$clubId/teams") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateTeamRequest("First Team", "Our best team"))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val team = response.body<Team>()
        assertEquals("First Team", team.name)
        assertEquals(clubId, team.clubId)
    }

    @Test
    fun `get team success`() = withPlaybookTestApplication {
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val auth = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("teamget@example.com", "password123", "Creator"))
        }.body<AuthResponse>()

        val clubId = client.post("/clubs") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateClubRequest("Team Club"))
        }.body<Club>().id

        val teamId = client.post("/clubs/$clubId/teams") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateTeamRequest("Get Me"))
        }.body<Team>().id

        val response = client.get("/teams/$teamId") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Get Me", response.body<Team>().name)
    }

    @Test
    fun `update team name success`() = withPlaybookTestApplication {
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val auth = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("teamupd@example.com", "password123", "Creator"))
        }.body<AuthResponse>()

        val clubId = client.post("/clubs") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateClubRequest("Team Club"))
        }.body<Club>().id

        val teamId = client.post("/clubs/$clubId/teams") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateTeamRequest("Old Name"))
        }.body<Team>().id

        val response = client.patch("/teams/$teamId") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(UpdateTeamRequest(name = "New Team Name"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("New Team Name", response.body<Team>().name)
    }

    @Test
    fun `archive team success`() = withPlaybookTestApplication {
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val auth = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("teamarc@example.com", "password123", "Creator"))
        }.body<AuthResponse>()

        val clubId = client.post("/clubs") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateClubRequest("Team Club"))
        }.body<Club>().id

        val teamId = client.post("/clubs/$clubId/teams") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateTeamRequest("To Archive"))
        }.body<Team>().id

        val response = client.delete("/teams/$teamId") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertNotNull(response.body<Team>().archivedAt)
        
        // Check that it's hidden in active listing
        val listResponse = client.get("/clubs/$clubId/teams") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
        }
        val teams = listResponse.body<List<Team>>()
        assertEquals(0, teams.size)
    }

    @Test
    fun `get team members`() = withPlaybookTestApplication {
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val auth = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("teammem@example.com", "password123", "Creator"))
        }.body<AuthResponse>()

        val clubId = client.post("/clubs") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateClubRequest("Team Club"))
        }.body<Club>().id

        val teamId = client.post("/clubs/$clubId/teams") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateTeamRequest("Member Team"))
        }.body<Team>().id

        val response = client.get("/teams/$teamId/members") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        // Currently empty because we haven't implemented join team yet, 
        // but the endpoint should return 200 and empty list.
        val members = response.body<List<TeamMember>>()
        assertEquals(0, members.size)
    }
}
