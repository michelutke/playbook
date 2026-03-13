package com.playbook.routes

import com.playbook.domain.models.Club
import com.playbook.domain.models.Team
import com.playbook.test.IntegrationTestBase
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ClubRoutesTest : IntegrationTestBase() {

    @Test
    fun `create club success`() = withPlaybookTestApplication {
        val client = createJsonClient()

        // 1. Register & Login
        val auth = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("club@example.com", "password123", "Club Creator"))
        }.body<AuthResponse>()

        // 2. Create Club
        val response = client.post("/clubs") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateClubRequest("Volley Masters", "volleyball", "Zurich"))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val club = response.body<Club>()
        assertEquals("Volley Masters", club.name)
        assertEquals("Zurich", club.location)
    }

    @Test
    fun `get club returns correct data`() = withPlaybookTestApplication {
        val client = createJsonClient()

        val auth = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("getclub@example.com", "password123", "Club Creator"))
        }.body<AuthResponse>()

        val clubId = client.post("/clubs") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateClubRequest("Get Club Inc", "volleyball", "Bern"))
        }.body<Club>().id

        val response = client.get("/clubs/$clubId") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val club = response.body<Club>()
        assertEquals("Get Club Inc", club.name)
    }

    @Test
    fun `update club name success`() = withPlaybookTestApplication {
        val client = createJsonClient()

        val auth = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("updclub@example.com", "password123", "Club Creator"))
        }.body<AuthResponse>()

        val clubId = client.post("/clubs") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateClubRequest("Old Name", "volleyball", "Geneva"))
        }.body<Club>().id

        val response = client.patch("/clubs/$clubId") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(UpdateClubRequest(name = "New Name"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val club = response.body<Club>()
        assertEquals("New Name", club.name)
        assertEquals("Geneva", club.location)
    }

    @Test
    fun `upload logo stored successfully`() = withPlaybookTestApplication {
        val client = createJsonClient()

        val auth = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("logoclub@example.com", "password123", "Club Creator"))
        }.body<AuthResponse>()

        val clubId = client.post("/clubs") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateClubRequest("Logo Club", "volleyball", "Lucerne"))
        }.body<Club>().id

        val response = client.post("/clubs/$clubId/logo") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            setBody(MultiPartFormDataContent(
                formData {
                    append("logo", "fake-image-content".toByteArray(), Headers.build {
                        append(HttpHeaders.ContentType, "image/png")
                        append(HttpHeaders.ContentDisposition, "filename=\"logo.png\"")
                    })
                }
            ))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val club = response.body<Club>()
        assertNotNull(club.logoPath)
    }

    @Test
    fun `list teams in club`() = withPlaybookTestApplication {
        val client = createJsonClient()

        val auth = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("listteams@example.com", "password123", "Club Creator"))
        }.body<AuthResponse>()

        val clubId = client.post("/clubs") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateClubRequest("Multi-Team Club", "volleyball", "Basel"))
        }.body<Club>().id

        client.post("/clubs/$clubId/teams") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateTeamRequest("Team A"))
        }

        client.post("/clubs/$clubId/teams") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateTeamRequest("Team B"))
        }

        val response = client.get("/clubs/$clubId/teams") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val teams = response.body<List<Team>>()
        assertEquals(2, teams.size)
    }
}
