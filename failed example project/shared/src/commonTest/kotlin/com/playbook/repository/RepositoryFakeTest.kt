package com.playbook.repository

import com.playbook.domain.Club
import com.playbook.domain.ClubStatus
import com.playbook.domain.CreateTeamRequest
import com.playbook.domain.Notification
import com.playbook.domain.PagedNotifications
import com.playbook.domain.RejectTeamRequest
import com.playbook.domain.Team
import com.playbook.domain.TeamStatus
import com.playbook.domain.UpdateTeamRequest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Instant

// --- Fake implementations ---

private val NOW = Instant.fromEpochSeconds(0)

private fun makeTeam(id: String, clubId: String = "c1", name: String = "Team $id") = Team(
    id = id,
    clubId = clubId,
    name = name,
    description = null,
    status = TeamStatus.ACTIVE,
    requestedBy = null,
    rejectionReason = null,
    createdAt = NOW,
    updatedAt = NOW,
)

class FakeTeamRepository : TeamRepository {
    private val store = mutableMapOf<String, Team>()
    private var idCounter = 0

    override suspend fun listByClub(clubId: String, statuses: List<TeamStatus>): List<Team> =
        store.values.filter { it.clubId == clubId && it.status in statuses }

    override suspend fun getById(teamId: String): Team? = store[teamId]

    override suspend fun create(clubId: String, request: CreateTeamRequest): Team {
        val team = makeTeam(id = "gen-${++idCounter}", clubId = clubId, name = request.name)
        store[team.id] = team
        return team
    }

    override suspend fun submitRequest(
        clubId: String,
        request: CreateTeamRequest,
        requestedByUserId: String,
    ): Team {
        val team = makeTeam(id = "gen-${++idCounter}", clubId = clubId, name = request.name)
            .copy(status = TeamStatus.PENDING, requestedBy = requestedByUserId)
        store[team.id] = team
        return team
    }

    override suspend fun update(teamId: String, request: UpdateTeamRequest): Team {
        val existing = store.getValue(teamId)
        val updated = existing.copy(name = request.name ?: existing.name)
        store[teamId] = updated
        return updated
    }

    override suspend fun setStatus(teamId: String, status: TeamStatus): Team {
        val updated = store.getValue(teamId).copy(status = status)
        store[teamId] = updated
        return updated
    }

    override suspend fun approve(teamId: String): Team = setStatus(teamId, TeamStatus.ACTIVE)

    override suspend fun reject(teamId: String, request: RejectTeamRequest): Team {
        val updated = store.getValue(teamId)
            .copy(status = TeamStatus.REJECTED, rejectionReason = request.reason)
        store[teamId] = updated
        return updated
    }

    override suspend fun delete(teamId: String) {
        store.remove(teamId)
    }

    fun reset() { store.clear(); idCounter = 0 }
}

class FakeNotificationRepository : NotificationRepository {
    private val store = mutableListOf<Notification>()
    private val unreadCount = MutableStateFlow(0)

    private fun refreshCount() { unreadCount.value = store.count { !it.read } }

    override suspend fun getNotifications(page: Int, limit: Int): PagedNotifications {
        val items = store.drop(page * limit).take(limit)
        return PagedNotifications(items = items, page = page, total = store.size)
    }

    override suspend fun markRead(notificationId: String) {
        val idx = store.indexOfFirst { it.id == notificationId }
        if (idx >= 0) store[idx] = store[idx].copy(read = true)
        refreshCount()
    }

    override suspend fun markAllRead() {
        store.replaceAll { it.copy(read = true) }
        refreshCount()
    }

    override suspend fun deleteNotification(notificationId: String) {
        store.removeAll { it.id == notificationId }
        refreshCount()
    }

    override fun getUnreadCount(): Flow<Int> = unreadCount

    override suspend fun syncFromServer() { /* no-op in fake */ }

    fun seed(vararg notifications: Notification) {
        store.addAll(notifications)
        refreshCount()
    }

    fun reset() { store.clear(); refreshCount() }
}

// --- Tests ---

class RepositoryFakeTest : StringSpec({

    val teamRepo = FakeTeamRepository()

    beforeTest { teamRepo.reset() }

    "FakeTeamRepository create adds team to store" {
        val team = teamRepo.create("c1", CreateTeamRequest(name = "Alpha"))
        team.name shouldBe "Alpha"
        teamRepo.getById(team.id) shouldBe team
    }

    "FakeTeamRepository listByClub returns only matching club teams" {
        teamRepo.create("c1", CreateTeamRequest(name = "A"))
        teamRepo.create("c2", CreateTeamRequest(name = "B"))
        val list = teamRepo.listByClub("c1")
        list.size shouldBe 1
        list.first().name shouldBe "A"
    }

    "FakeTeamRepository update changes team name" {
        val team = teamRepo.create("c1", CreateTeamRequest(name = "Old"))
        val updated = teamRepo.update(team.id, UpdateTeamRequest(name = "New"))
        updated.name shouldBe "New"
    }

    "FakeTeamRepository delete removes team" {
        val team = teamRepo.create("c1", CreateTeamRequest(name = "ToDelete"))
        teamRepo.delete(team.id)
        teamRepo.getById(team.id) shouldBe null
    }

    "FakeTeamRepository approve sets status to ACTIVE" {
        val team = teamRepo.submitRequest("c1", CreateTeamRequest(name = "Req"), "u1")
        team.status shouldBe TeamStatus.PENDING
        val approved = teamRepo.approve(team.id)
        approved.status shouldBe TeamStatus.ACTIVE
    }

    "FakeTeamRepository reject sets status to REJECTED with reason" {
        val team = teamRepo.submitRequest("c1", CreateTeamRequest(name = "Req"), "u1")
        val rejected = teamRepo.reject(team.id, RejectTeamRequest(reason = "Duplicate"))
        rejected.status shouldBe TeamStatus.REJECTED
        rejected.rejectionReason shouldBe "Duplicate"
    }

    "FakeTeamRepository getById returns null for unknown id" {
        teamRepo.getById("unknown") shouldBe null
    }

    // --- Notification fake tests ---

    val notifRepo = FakeNotificationRepository()

    beforeTest { notifRepo.reset() }

    "FakeNotificationRepository getNotifications returns seeded items" {
        val n = Notification("n1", "u1", "event", "T", "B", "link", null, false, "2025-01-01")
        notifRepo.seed(n)
        val result = notifRepo.getNotifications()
        result.items shouldContain n
        result.total shouldBe 1
    }

    "FakeNotificationRepository markRead reduces unread count" {
        val n = Notification("n2", "u1", "event", "T", "B", "link", null, false, "2025-01-01")
        notifRepo.seed(n)
        notifRepo.markRead("n2")
        val result = notifRepo.getNotifications()
        result.items.first().read shouldBe true
    }

    "FakeNotificationRepository markAllRead marks every notification read" {
        notifRepo.seed(
            Notification("n3", "u1", "event", "T", "B", "l", null, false, "2025-01-01"),
            Notification("n4", "u1", "event", "T", "B", "l", null, false, "2025-01-01"),
        )
        notifRepo.markAllRead()
        val result = notifRepo.getNotifications()
        result.items.all { it.read } shouldBe true
    }

    "FakeNotificationRepository deleteNotification removes item" {
        val n = Notification("n5", "u1", "event", "T", "B", "link", null, false, "2025-01-01")
        notifRepo.seed(n)
        notifRepo.deleteNotification("n5")
        notifRepo.getNotifications().items.shouldBeEmpty()
    }
})
