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

class AuthRoutesTest : IntegrationTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun seed() {
            transaction { Fixtures.seedCoachAndTeam(this) }
        }
    }

    @Test
    fun `register_newUser_returns201WithToken`() = testApp {
        val client = jsonClient()
        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"newuser@test.com","password":"pass123","displayName":"New User"}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("token"))
        assertTrue(body.contains("userId"))
    }

    @Test
    fun `register_blankEmail_returns400`() = testApp {
        val client = jsonClient()
        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"","password":"pass123"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `register_blankPassword_returns400`() = testApp {
        val client = jsonClient()
        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"test@test.com","password":""}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `login_validCredentials_returns200WithToken`() = testApp {
        // Register first
        val client = jsonClient()
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"logintest@test.com","password":"password123"}""")
        }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"logintest@test.com","password":"password123"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("token"))
    }

    @Test
    fun `login_wrongPassword_returns401`() = testApp {
        val client = jsonClient()
        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"${Fixtures.COACH_EMAIL}","password":"wrongpassword"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `login_unknownEmail_returns401`() = testApp {
        val client = jsonClient()
        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"nobody@nowhere.com","password":"pass"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `getUsersMe_withValidToken_returns200WithProfile`() = testApp {
        val client = jsonClient()
        val response = client.get("/users/me") {
            bearerAuth(bearerToken(Fixtures.TEST_COACH_ID))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains(Fixtures.COACH_EMAIL))
    }

    @Test
    fun `getUsersMe_noToken_returns401`() = testApp {
        val response = createClient {}.get("/users/me")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
