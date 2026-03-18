package ch.teamorg.routes

import ch.teamorg.domain.models.User
import ch.teamorg.test.IntegrationTestBase
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuthRoutesTest : IntegrationTestBase() {

    @Test
    fun `register success`() = withTeamorgTestApplication {
        val client = createJsonClient()

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("test@example.com", "password123", "Test User"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val authResponse = response.body<AuthResponse>()
        assertNotNull(authResponse.token)
        assertEquals("Test User", authResponse.displayName)
    }

    @Test
    fun `register duplicate email returns 409`() = withTeamorgTestApplication {
        val client = createJsonClient()

        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("dup@example.com", "password123", "User 1"))
        }

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("dup@example.com", "password123", "User 2"))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `register invalid email returns 400`() = withTeamorgTestApplication {
        val client = createJsonClient()

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("invalid-email", "password123", "User"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `register short password returns 400`() = withTeamorgTestApplication {
        val client = createJsonClient()

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("test2@example.com", "short", "User"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `login success returns JWT`() = withTeamorgTestApplication {
        val client = createJsonClient()

        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("login@example.com", "password123", "Login User"))
        }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("login@example.com", "password123"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val authResponse = response.body<AuthResponse>()
        assertNotNull(authResponse.token)
    }

    @Test
    fun `login wrong password returns 401`() = withTeamorgTestApplication {
        val client = createJsonClient()

        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("wrongpass@example.com", "password123", "User"))
        }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("wrongpass@example.com", "wrongpassword"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `login unknown email returns 401`() = withTeamorgTestApplication {
        val client = createJsonClient()

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("unknown@example.com", "password123"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET auth me authenticated returns user`() = withTeamorgTestApplication {
        val client = createJsonClient()

        val regResponse = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("me@example.com", "password123", "Me User"))
        }.body<AuthResponse>()

        val response = client.get("/auth/me") {
            header(HttpHeaders.Authorization, "Bearer ${regResponse.token}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val user = response.body<User>()
        assertEquals("Me User", user.displayName)
        assertEquals("me@example.com", user.email)
    }

    @Test
    fun `GET auth me no token returns 401`() = withTeamorgTestApplication {
        val client = createJsonClient()

        val response = client.get("/auth/me")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `logout with valid token returns 200`() = withTeamorgTestApplication {
        val client = createJsonClient()

        val auth = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("logout@example.com", "password123", "Logout User"))
        }.body<AuthResponse>()

        val response = client.post("/auth/logout") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `logout without token returns 401`() = withTeamorgTestApplication {
        val client = createJsonClient()

        val response = client.post("/auth/logout")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `login returns userId and displayName`() = withTeamorgTestApplication {
        val client = createJsonClient()

        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("profile@example.com", "password123", "Profile User"))
        }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("profile@example.com", "password123"))
        }.body<AuthResponse>()

        assertNotNull(response.userId)
        assertEquals("Profile User", response.displayName)
    }

    @Test
    fun `register blank display name returns 400`() = withTeamorgTestApplication {
        val client = createJsonClient()

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("blank@example.com", "password123", "  "))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
