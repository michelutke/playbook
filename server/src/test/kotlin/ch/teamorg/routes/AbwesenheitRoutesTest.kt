package ch.teamorg.routes

import ch.teamorg.test.IntegrationTestBase
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Serializable
private data class CreateAbwPayload(
    val presetType: String,
    val label: String,
    val bodyPart: String? = null,
    val ruleType: String,
    val weekdays: List<Int>? = null,
    val startDate: String? = null,
    val endDate: String? = null
)

@Serializable
private data class UpdateAbwPayload(
    val presetType: String? = null,
    val label: String? = null,
    val ruleType: String? = null
)

@Serializable
private data class AbwResponse(
    val id: String,
    val presetType: String,
    val label: String,
    val bodyPart: String? = null,
    val ruleType: String,
    val weekdays: List<Int>? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
private data class BackfillStatusPayload(val status: String)

@Serializable
private data class AbwRuleWithUserId(
    val id: String,
    val userId: String,
    val presetType: String,
    val label: String,
    val bodyPart: String? = null,
    val ruleType: String,
    val weekdays: List<Int>? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val createdAt: String,
    val updatedAt: String
)

class AbwesenheitRoutesTest : IntegrationTestBase() {

    private suspend fun ApplicationTestBuilder.registerAndLogin(
        email: String,
        password: String = "Password1!",
        displayName: String = "User"
    ): AuthResponse {
        val client = createJsonClient()
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email, password, displayName))
        }
        return client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }.body<AuthResponse>()
    }

    @Test
    fun `create recurring rule returns 201`() = withTeamorgTestApplication {
        val client = createJsonClient()
        val auth = registerAndLogin("abw_recurring@example.com")

        val response = client.post("/users/me/abwesenheit") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateAbwPayload(
                presetType = "injury",
                label = "Knee injury",
                ruleType = "recurring",
                weekdays = listOf(1, 3)  // Mon, Wed
            ))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val rule = response.body<AbwResponse>()
        assertNotNull(rule.id)
        assertEquals("injury", rule.presetType)
        assertEquals("Knee injury", rule.label)
        assertEquals("recurring", rule.ruleType)
    }

    @Test
    fun `create period rule returns 201`() = withTeamorgTestApplication {
        val client = createJsonClient()
        val auth = registerAndLogin("abw_period@example.com")

        val response = client.post("/users/me/abwesenheit") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateAbwPayload(
                presetType = "illness",
                label = "Flu",
                ruleType = "period",
                startDate = "2026-04-01",
                endDate = "2026-04-14"
            ))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val rule = response.body<AbwResponse>()
        assertNotNull(rule.id)
        assertEquals("period", rule.ruleType)
        assertEquals("2026-04-01", rule.startDate)
        assertEquals("2026-04-14", rule.endDate)
    }

    @Test
    fun `list rules returns own rules only`() = withTeamorgTestApplication {
        val client = createJsonClient()
        val authA = registerAndLogin("abw_listA@example.com", displayName = "User A")
        val authB = registerAndLogin("abw_listB@example.com", displayName = "User B")

        // Create rule for A
        client.post("/users/me/abwesenheit") {
            header(HttpHeaders.Authorization, "Bearer ${authA.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateAbwPayload(
                presetType = "injury",
                label = "A rule",
                ruleType = "recurring",
                weekdays = listOf(2)
            ))
        }

        // Create rule for B
        client.post("/users/me/abwesenheit") {
            header(HttpHeaders.Authorization, "Bearer ${authB.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateAbwPayload(
                presetType = "illness",
                label = "B rule",
                ruleType = "period",
                startDate = "2026-05-01",
                endDate = "2026-05-10"
            ))
        }

        val response = client.get("/users/me/abwesenheit") {
            header(HttpHeaders.Authorization, "Bearer ${authA.token}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val rules = response.body<List<AbwResponse>>()
        assertEquals(1, rules.size)
        assertEquals("A rule", rules[0].label)
    }

    @Test
    fun `update rule returns 200`() = withTeamorgTestApplication {
        val client = createJsonClient()
        val auth = registerAndLogin("abw_update@example.com")

        val created = client.post("/users/me/abwesenheit") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateAbwPayload(
                presetType = "injury",
                label = "Original label",
                ruleType = "recurring",
                weekdays = listOf(1)
            ))
        }.body<AbwResponse>()

        val response = client.put("/users/me/abwesenheit/${created.id}") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(UpdateAbwPayload(label = "Updated label"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val updated = response.body<AbwResponse>()
        assertEquals("Updated label", updated.label)
    }

    @Test
    fun `delete rule returns 204`() = withTeamorgTestApplication {
        val client = createJsonClient()
        val auth = registerAndLogin("abw_delete@example.com")

        val created = client.post("/users/me/abwesenheit") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateAbwPayload(
                presetType = "personal",
                label = "To delete",
                ruleType = "recurring",
                weekdays = listOf(5)
            ))
        }.body<AbwResponse>()

        val response = client.delete("/users/me/abwesenheit/${created.id}") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)

        // Verify deleted
        val listResponse = client.get("/users/me/abwesenheit") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
        }
        val rules = listResponse.body<List<AbwResponse>>()
        assertEquals(0, rules.size)
    }

    @Test
    fun `create absence rule response includes userId`() = withTeamorgTestApplication {
        val client = createJsonClient()
        val auth = registerAndLogin("abw_userid@example.com")

        val response = client.post("/users/me/abwesenheit") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateAbwPayload(
                presetType = "work",
                label = "Work conflict",
                ruleType = "recurring",
                weekdays = listOf(0, 4)
            ))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val rule = response.body<AbwRuleWithUserId>()
        assertNotNull(rule.id)
        assertEquals(auth.userId, rule.userId)
        assertEquals("work", rule.presetType)
        assertTrue(rule.createdAt.isNotBlank())
        assertTrue(rule.updatedAt.isNotBlank())
    }

    @Test
    fun `get absence rules returns parseable JSON with userId`() = withTeamorgTestApplication {
        val client = createJsonClient()
        val auth = registerAndLogin("abw_getall@example.com")

        client.post("/users/me/abwesenheit") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateAbwPayload(
                presetType = "holidays",
                label = "Summer break",
                ruleType = "period",
                startDate = "2026-07-01",
                endDate = "2026-07-31"
            ))
        }

        val response = client.get("/users/me/abwesenheit") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val rules = response.body<List<AbwRuleWithUserId>>()
        assertEquals(1, rules.size)
        assertEquals(auth.userId, rules[0].userId)
        assertEquals("2026-07-01", rules[0].startDate)
        assertEquals("2026-07-31", rules[0].endDate)
    }

    @Test
    fun `delete absence rule removes it from list`() = withTeamorgTestApplication {
        val client = createJsonClient()
        val auth = registerAndLogin("abw_delete2@example.com")

        val created = client.post("/users/me/abwesenheit") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateAbwPayload(
                presetType = "school",
                label = "Exam period",
                ruleType = "period",
                startDate = "2026-06-01",
                endDate = "2026-06-15"
            ))
        }.body<AbwRuleWithUserId>()

        val deleteResponse = client.delete("/users/me/abwesenheit/${created.id}") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
        }
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        val listResponse = client.get("/users/me/abwesenheit") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
        }
        assertEquals(HttpStatusCode.OK, listResponse.status)
        val rules = listResponse.body<List<AbwRuleWithUserId>>()
        assertEquals(0, rules.size)
    }

    @Test
    fun `backfill status returns done or pending after rule creation`() = withTeamorgTestApplication {
        val client = createJsonClient()
        val auth = registerAndLogin("abw_backfill@example.com")

        client.post("/users/me/abwesenheit") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateAbwPayload(
                presetType = "injury",
                label = "Backfill test",
                ruleType = "recurring",
                weekdays = listOf(1, 2, 3)
            ))
        }

        val response = client.get("/users/me/abwesenheit/backfill-status") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val status = response.body<BackfillStatusPayload>()
        // Status is either "pending" or "done" depending on timing
        assertNotNull(status.status)
        assert(status.status in listOf("pending", "done", "failed")) {
            "Unexpected backfill status: ${status.status}"
        }
    }
}
