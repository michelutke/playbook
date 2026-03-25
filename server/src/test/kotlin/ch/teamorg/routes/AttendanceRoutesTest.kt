package ch.teamorg.routes

import ch.teamorg.domain.models.Club
import ch.teamorg.domain.models.Event
import ch.teamorg.domain.models.Team
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
private data class AttendanceSubmitPayload(val status: String, val reason: String? = null)

@Serializable
private data class AttendanceResponsePayload(
    val eventId: String,
    val userId: String,
    val status: String,
    val reason: String? = null
)

@Serializable
private data class CheckInOverridePayload(val status: String, val note: String? = null)

@Serializable
private data class CheckInRowPayload(
    val eventId: String,
    val userId: String,
    val status: String,
    val note: String? = null,
    val setBy: String,
    val previousStatus: String? = null
)

@Serializable
private data class CreateEventPayloadAttn(
    val title: String,
    val type: String,
    val startAt: String,
    val endAt: String,
    val responseDeadline: String? = null,
    val teamIds: List<String> = emptyList()
)

class AttendanceRoutesTest : IntegrationTestBase() {

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

    private suspend fun ApplicationTestBuilder.setupClubAndTeam(token: String): Pair<String, String> {
        val client = createJsonClient()
        val club = client.post("/clubs") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CreateClubRequest("Attendance Club"))
        }.body<Club>()
        val team = client.post("/clubs/${club.id}/teams") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CreateTeamRequest("Attendance Team"))
        }.body<Team>()
        return Pair(club.id, team.id)
    }

    private suspend fun ApplicationTestBuilder.createEvent(
        token: String,
        title: String,
        responseDeadline: String? = null,
        teamIds: List<String> = emptyList()
    ): Event {
        val client = createJsonClient()
        return client.post("/events") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CreateEventPayloadAttn(
                title = title,
                type = "training",
                startAt = "2026-09-01T10:00:00Z",
                endAt = "2026-09-01T12:00:00Z",
                responseDeadline = responseDeadline,
                teamIds = teamIds
            ))
        }.body<Event>()
    }

    @Test
    fun `submit response returns 200 with valid status`() = withTeamorgTestApplication {
        val client = createJsonClient()
        val auth = registerAndLogin("att_ok@example.com")
        val event = createEvent(auth.token, "Training Ok")

        val response = client.put("/events/${event.id}/attendance/me") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(AttendanceSubmitPayload(status = "confirmed"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<AttendanceResponsePayload>()
        assertEquals("confirmed", body.status)
        assertEquals(event.id.toString(), body.eventId)
    }

    @Test
    fun `submit unsure without reason returns 400`() = withTeamorgTestApplication {
        val client = createJsonClient()
        val auth = registerAndLogin("att_unsure_nok@example.com")
        val event = createEvent(auth.token, "Training Unsure Bad")

        val response = client.put("/events/${event.id}/attendance/me") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(AttendanceSubmitPayload(status = "unsure"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `submit unsure with reason returns 200`() = withTeamorgTestApplication {
        val client = createJsonClient()
        val auth = registerAndLogin("att_unsure_ok@example.com")
        val event = createEvent(auth.token, "Training Unsure Good")

        val response = client.put("/events/${event.id}/attendance/me") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(AttendanceSubmitPayload(status = "unsure", reason = "might be late"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<AttendanceResponsePayload>()
        assertEquals("unsure", body.status)
    }

    @Test
    fun `submit after deadline returns 409`() = withTeamorgTestApplication {
        val client = createJsonClient()
        val auth = registerAndLogin("att_deadline@example.com")
        // Past deadline
        val event = createEvent(auth.token, "Training Past Deadline", responseDeadline = "2020-01-01T00:00:00Z")

        val response = client.put("/events/${event.id}/attendance/me") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(AttendanceSubmitPayload(status = "confirmed"))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `get event attendance returns all responses`() = withTeamorgTestApplication {
        val client = createJsonClient()
        val auth1 = registerAndLogin("att_list1@example.com", displayName = "Player 1")
        val auth2 = registerAndLogin("att_list2@example.com", displayName = "Player 2")
        val event = createEvent(auth1.token, "Training List")

        client.put("/events/${event.id}/attendance/me") {
            header(HttpHeaders.Authorization, "Bearer ${auth1.token}")
            contentType(ContentType.Application.Json)
            setBody(AttendanceSubmitPayload(status = "confirmed"))
        }
        client.put("/events/${event.id}/attendance/me") {
            header(HttpHeaders.Authorization, "Bearer ${auth2.token}")
            contentType(ContentType.Application.Json)
            setBody(AttendanceSubmitPayload(status = "declined"))
        }

        val response = client.get("/events/${event.id}/attendance") {
            header(HttpHeaders.Authorization, "Bearer ${auth1.token}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val entries = response.body<List<AttendanceResponsePayload>>()
        assertEquals(2, entries.size)
    }

    @Test
    fun `coach can override attendance`() = withTeamorgTestApplication {
        val client = createJsonClient()
        val coachAuth = registerAndLogin("coach_override@example.com", displayName = "Coach")
        val playerAuth = registerAndLogin("player_target@example.com", displayName = "Player")
        setupClubAndTeam(coachAuth.token)
        val event = createEvent(coachAuth.token, "Training Override")

        val response = client.put("/events/${event.id}/check-in/${playerAuth.userId}") {
            header(HttpHeaders.Authorization, "Bearer ${coachAuth.token}")
            contentType(ContentType.Application.Json)
            setBody(CheckInOverridePayload(status = "present"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val record = response.body<CheckInRowPayload>()
        assertEquals("present", record.status)
        assertEquals(playerAuth.userId, record.userId)
    }

    @Test
    fun `non-coach cannot override attendance`() = withTeamorgTestApplication {
        val client = createJsonClient()
        val ownerAuth = registerAndLogin("owner_checkin@example.com", displayName = "Owner")
        val playerAuth = registerAndLogin("player_nocoach@example.com", displayName = "Player")
        val event = createEvent(ownerAuth.token, "Training No Coach")

        // playerAuth has no coach role
        val response = client.put("/events/${event.id}/check-in/${ownerAuth.userId}") {
            header(HttpHeaders.Authorization, "Bearer ${playerAuth.token}")
            contentType(ContentType.Application.Json)
            setBody(CheckInOverridePayload(status = "absent"))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `override captures previous status`() = withTeamorgTestApplication {
        val client = createJsonClient()
        val coachAuth = registerAndLogin("coach_audit@example.com", displayName = "Coach Audit")
        val playerAuth = registerAndLogin("player_audit@example.com", displayName = "Player Audit")
        setupClubAndTeam(coachAuth.token)
        val event = createEvent(coachAuth.token, "Training Audit")

        // First override: present
        client.put("/events/${event.id}/check-in/${playerAuth.userId}") {
            header(HttpHeaders.Authorization, "Bearer ${coachAuth.token}")
            contentType(ContentType.Application.Json)
            setBody(CheckInOverridePayload(status = "present"))
        }

        // Second override: absent — should capture previous_status = present
        val response = client.put("/events/${event.id}/check-in/${playerAuth.userId}") {
            header(HttpHeaders.Authorization, "Bearer ${coachAuth.token}")
            contentType(ContentType.Application.Json)
            setBody(CheckInOverridePayload(status = "absent"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val record = response.body<CheckInRowPayload>()
        assertEquals("absent", record.status)
        assertEquals("present", record.previousStatus)
    }

    @Serializable
    private data class CheckInEntryPayload(
        val userId: String,
        val userName: String,
        val userAvatar: String? = null,
        val response: AttendanceResponseDtoPayload? = null,
        val record: AttendanceRecordDtoPayload? = null
    )

    @Serializable
    private data class AttendanceResponseDtoPayload(
        val eventId: String,
        val userId: String,
        val status: String,
        val reason: String? = null,
        val abwesenheitRuleId: String? = null,
        val manualOverride: Boolean = false,
        val respondedAt: String? = null,
        val updatedAt: String
    )

    @Serializable
    private data class AttendanceRecordDtoPayload(
        val eventId: String,
        val userId: String,
        val status: String,
        val note: String? = null,
        val setBy: String,
        val setAt: String,
        val previousStatus: String? = null,
        val previousSetBy: String? = null
    )

    @Test
    fun `get check-in entries returns team members with responses`() = withTeamorgTestApplication {
        val client = createJsonClient()
        val coachAuth = registerAndLogin("ci_coach@example.com", displayName = "Coach")
        val player1Auth = registerAndLogin("ci_player1@example.com", displayName = "Player One")
        val player2Auth = registerAndLogin("ci_player2@example.com", displayName = "Player Two")

        val (clubId, teamId) = setupClubAndTeam(coachAuth.token)

        // Create player invite and redeem for both players
        val invite1Token = client.post("/teams/$teamId/invites") {
            header(HttpHeaders.Authorization, "Bearer ${coachAuth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateInviteRequest(role = "player"))
        }.body<InviteResponse>().token

        client.post("/invites/$invite1Token/redeem") {
            header(HttpHeaders.Authorization, "Bearer ${player1Auth.token}")
        }

        val invite2Token = client.post("/teams/$teamId/invites") {
            header(HttpHeaders.Authorization, "Bearer ${coachAuth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateInviteRequest(role = "player"))
        }.body<InviteResponse>().token

        client.post("/invites/$invite2Token/redeem") {
            header(HttpHeaders.Authorization, "Bearer ${player2Auth.token}")
        }

        // Create event linked to team
        val event = createEvent(coachAuth.token, "Training CheckIn Entries", teamIds = listOf(teamId))

        // Player 1 submits confirmed response
        client.put("/events/${event.id}/attendance/me") {
            header(HttpHeaders.Authorization, "Bearer ${player1Auth.token}")
            contentType(ContentType.Application.Json)
            setBody(AttendanceSubmitPayload(status = "confirmed"))
        }

        val response = client.get("/events/${event.id}/check-in") {
            header(HttpHeaders.Authorization, "Bearer ${coachAuth.token}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val entries = response.body<List<CheckInEntryPayload>>()
        assertEquals(2, entries.size)

        val player1Entry = entries.find { it.userId == player1Auth.userId }
        assertNotNull(player1Entry)
        assertEquals("Player One", player1Entry.userName)
        assertEquals("confirmed", player1Entry.response?.status)

        val player2Entry = entries.find { it.userId == player2Auth.userId }
        assertNotNull(player2Entry)
        assertEquals("Player Two", player2Entry.userName)
    }

    @Test
    fun `get event attendance returns correct DTO fields`() = withTeamorgTestApplication {
        val client = createJsonClient()
        val auth = registerAndLogin("dto_check@example.com", displayName = "DTO User")
        val event = createEvent(auth.token, "Training DTO Check")

        client.put("/events/${event.id}/attendance/me") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(AttendanceSubmitPayload(status = "confirmed"))
        }

        val response = client.get("/events/${event.id}/attendance") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val entries = response.body<List<AttendanceResponseDtoPayload>>()
        assertEquals(1, entries.size)

        val entry = entries[0]
        assertEquals(event.id.toString(), entry.eventId)
        assertEquals(auth.userId, entry.userId)
        assertEquals("confirmed", entry.status)
        // updatedAt must be a non-blank ISO string, not a UUID or raw java.time value
        assertTrue(entry.updatedAt.isNotBlank())
        assertTrue(entry.updatedAt.contains("T"), "updatedAt should be ISO-8601: ${entry.updatedAt}")
    }

    @Test
    fun `submit response updates existing response`() = withTeamorgTestApplication {
        val client = createJsonClient()
        val auth = registerAndLogin("update_response@example.com", displayName = "Updater")
        val event = createEvent(auth.token, "Training Update Response")

        client.put("/events/${event.id}/attendance/me") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(AttendanceSubmitPayload(status = "confirmed"))
        }

        client.put("/events/${event.id}/attendance/me") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            contentType(ContentType.Application.Json)
            setBody(AttendanceSubmitPayload(status = "declined"))
        }

        val meResponse = client.get("/events/${event.id}/attendance/me") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
        }

        assertEquals(HttpStatusCode.OK, meResponse.status)
        val body = meResponse.body<AttendanceResponsePayload>()
        assertEquals("declined", body.status)
    }

    @Test
    fun `get my response returns NoContent when no response exists`() = withTeamorgTestApplication {
        val client = createJsonClient()
        val auth = registerAndLogin("no_response_yet@example.com", displayName = "No Response")
        val event = createEvent(auth.token, "Training No Response")

        val response = client.get("/events/${event.id}/attendance/me") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
    }
}
