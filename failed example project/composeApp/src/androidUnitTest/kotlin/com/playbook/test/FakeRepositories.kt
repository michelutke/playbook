package com.playbook.test

import com.playbook.domain.*
import com.playbook.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.Clock

// ─── Auth ───────────────────────────────────────────────────────────────────

class FakeAuthRepository(
    private val authResponse: AuthResponse = AuthResponse(token = "fake-token", userId = "user-1"),
    private val userProfile: UserProfile = UserProfile(
        id = "user-1",
        email = "test@example.com",
        displayName = null,
        clubId = null,
    ),
) : AuthRepository {
    override suspend fun register(request: RegisterRequest): AuthResponse = authResponse
    override suspend fun login(request: LoginRequest): AuthResponse = authResponse
    override suspend fun getMe(token: String): UserProfile = userProfile
}

class FailingAuthRepository : AuthRepository {
    override suspend fun register(request: RegisterRequest): AuthResponse =
        throw Exception("Authentication failed.")
    override suspend fun login(request: LoginRequest): AuthResponse =
        throw Exception("Authentication failed.")
    override suspend fun getMe(token: String): UserProfile =
        throw Exception("Not found")
}

// ─── Club ────────────────────────────────────────────────────────────────────

class FakeClubRepository(
    private val club: Club? = Club(
        id = "club-1",
        name = "Test Club",
        logoUrl = null,
        sportType = "Football",
        location = null,
        status = ClubStatus.ACTIVE,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
    ),
) : ClubRepository {
    override suspend fun create(request: CreateClubRequest, createdByUserId: String): Club = club!!
    override suspend fun getById(clubId: String): Club? = club
    override suspend fun update(clubId: String, request: UpdateClubRequest): Club = club!!
    override suspend fun updateLogoUrl(clubId: String, logoUrl: String): Club = club!!
    override suspend fun uploadLogo(clubId: String, contentType: String, imageBytes: ByteArray): Club = club!!
}

// ─── Team ────────────────────────────────────────────────────────────────────

class FakeTeamRepository(
    private val teams: List<Team> = emptyList(),
    private val team: Team? = null,
) : TeamRepository {
    override suspend fun listByClub(clubId: String, statuses: List<TeamStatus>): List<Team> =
        teams.filter { it.status in statuses }
    override suspend fun getById(teamId: String): Team? = team ?: teams.find { it.id == teamId }
    override suspend fun create(clubId: String, request: CreateTeamRequest): Team =
        Team(
            id = "team-new",
            clubId = clubId,
            name = request.name,
            description = request.description,
            status = TeamStatus.ACTIVE,
            requestedBy = null,
            rejectionReason = null,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
        )
    override suspend fun submitRequest(clubId: String, request: CreateTeamRequest, requestedByUserId: String): Team =
        create(clubId, request)
    override suspend fun update(teamId: String, request: UpdateTeamRequest): Team = getById(teamId)!!
    override suspend fun setStatus(teamId: String, status: TeamStatus): Team = getById(teamId)!!
    override suspend fun approve(teamId: String): Team = getById(teamId)!!
    override suspend fun reject(teamId: String, request: RejectTeamRequest): Team = getById(teamId)!!
    override suspend fun delete(teamId: String) {}
}

// ─── Membership ──────────────────────────────────────────────────────────────

class FakeMembershipRepository(
    private val roster: List<RosterMember> = emptyList(),
) : MembershipRepository {
    override suspend fun getRoster(teamId: String): List<RosterMember> = roster
    override suspend fun addRole(teamId: String, userId: String, role: MemberRole, addedByUserId: String) {}
    override suspend fun removeRole(teamId: String, userId: String, role: MemberRole) {}
    override suspend fun removeMember(teamId: String, userId: String) {}
    override suspend fun leaveTeam(teamId: String, userId: String) {}
    override suspend fun getProfile(teamId: String, userId: String): PlayerProfile? = null
    override suspend fun updateProfile(
        teamId: String,
        userId: String,
        request: UpdatePlayerProfileRequest,
    ): PlayerProfile = throw UnsupportedOperationException()
    override suspend fun hasRole(teamId: String, userId: String, role: MemberRole): Boolean = false
}

// ─── Event ───────────────────────────────────────────────────────────────────

class FakeEventRepository(
    private val events: List<Event> = emptyList(),
) : EventRepository {
    override suspend fun listForUser(
        userId: String,
        from: kotlinx.datetime.Instant?,
        to: kotlinx.datetime.Instant?,
        type: EventType?,
        teamId: String?,
    ): List<Event> = events
    override suspend fun listForTeam(
        teamId: String,
        from: kotlinx.datetime.Instant?,
        to: kotlinx.datetime.Instant?,
        type: EventType?,
    ): List<Event> = events
    override suspend fun getById(eventId: String): Event? = events.find { it.id == eventId }
    override suspend fun create(request: CreateEventRequest, createdBy: String): Event =
        throw UnsupportedOperationException()
    override suspend fun update(eventId: String, request: UpdateEventRequest): Event =
        throw UnsupportedOperationException()
    override suspend fun cancel(eventId: String, request: CancelEventRequest): Event =
        throw UnsupportedOperationException()
    override suspend fun duplicate(eventId: String): CreateEventRequest =
        throw UnsupportedOperationException()
    override suspend fun resolveTargetedUsers(eventId: String): List<String> = emptyList()
}

// ─── Attendance ──────────────────────────────────────────────────────────────

class FakeAttendanceRepository(
    private val teamAttendanceView: TeamAttendanceView = TeamAttendanceView(),
) : AttendanceRepository {
    override suspend fun getEventAttendance(eventId: String): TeamAttendanceView = teamAttendanceView
    override suspend fun getMyAttendance(eventId: String, userId: String): AttendanceResponse =
        throw UnsupportedOperationException()
    override suspend fun upsertAttendance(
        eventId: String,
        userId: String,
        request: UpdateAttendanceRequest,
    ): AttendanceResponse = throw UnsupportedOperationException()
    override fun observeMyAttendance(eventId: String): Flow<AttendanceResponse> = emptyFlow()
    override suspend fun getCheckInList(eventId: String): List<AttendanceEntry> = emptyList()
    override suspend fun setCheckIn(
        eventId: String,
        userId: String,
        request: UpdateCheckInRequest,
    ): AttendanceRecord = throw UnsupportedOperationException()
    override suspend fun getUserAttendance(
        userId: String,
        from: kotlinx.datetime.Instant?,
        to: kotlinx.datetime.Instant?,
    ): List<AttendanceRow> = emptyList()
    override suspend fun getTeamAttendance(
        teamId: String,
        from: kotlinx.datetime.Instant?,
        to: kotlinx.datetime.Instant?,
    ): List<AttendanceRow> = emptyList()
    override suspend fun triggerBackfillForEvent(eventId: String, teamId: String) {}
}

// ─── Abwesenheit ─────────────────────────────────────────────────────────────

class FakeAbwesenheitRepository(
    private val rules: List<AbwesenheitRule> = emptyList(),
) : AbwesenheitRepository {
    override suspend fun listRules(userId: String): List<AbwesenheitRule> = rules
    override suspend fun createRule(
        userId: String,
        request: CreateAbwesenheitRuleRequest,
    ): CreateAbwesenheitResponse = throw UnsupportedOperationException()
    override suspend fun updateRule(
        ruleId: String,
        userId: String,
        request: UpdateAbwesenheitRuleRequest,
    ): AbwesenheitRule = throw UnsupportedOperationException()
    override suspend fun deleteRule(ruleId: String, userId: String) {}
    override suspend fun getBackfillStatus(userId: String): BackfillJobStatus =
        BackfillJobStatus(status = "done")
    override fun pollBackfillStatus(intervalMs: Long): Flow<BackfillJobStatus> = emptyFlow()
}
