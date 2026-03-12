package com.playbook.db.repositories

import com.playbook.db.tables.*
import com.playbook.domain.sa.*
import com.playbook.infra.generateToken
import com.playbook.plugins.NotFoundException
import io.ktor.server.config.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.simplejavamail.api.mailer.Mailer
import java.io.File
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class SuperAdminRepositoryImpl(
    private val mailer: Mailer,
    private val config: ApplicationConfig,
) {
    private val emailScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // SA-012: dashboard stats
    suspend fun getStats(): SaStats = newSuspendedTransaction {
        val totalClubs = ClubsTable.selectAll().where { ClubsTable.deletedAt.isNull() }.count().toInt()
        val totalUsers = UsersTable.selectAll().count().toInt()
        val todayStart = OffsetDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toOffsetDateTime()
        val todayEnd = todayStart.plusDays(1)
        val activeEventsToday = try {
            EventsTable.selectAll().where {
                (EventsTable.startAt greaterEq todayStart) and
                (EventsTable.startAt less todayEnd) and
                (EventsTable.status eq "active")
            }.count().toInt()
        } catch (_: Exception) { 0 }
        val sevenDaysAgo = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7)
        val signUps = UsersTable.selectAll().where { UsersTable.createdAt greaterEq sevenDaysAgo }.count().toInt()
        SaStats(totalClubs, totalUsers, activeEventsToday, signUps)
    }

    // SA-013: list clubs (batched counts — no N+1)
    suspend fun listClubs(status: String?, search: String?): List<SaClub> = newSuspendedTransaction {
        var query = ClubsTable.selectAll().where { ClubsTable.deletedAt.isNull() }
        status?.let { s -> query = query.andWhere { ClubsTable.status eq s } }
        search?.let { q -> query = query.andWhere { ClubsTable.name.lowerCase() like "%${q.lowercase()}%" } }
        val clubs = query.toList()
        val clubIds = clubs.map { it[ClubsTable.id] }
        val counts = fetchClubCounts(clubIds)
        clubs.map { row ->
            val (mc, tc, mem) = counts[row[ClubsTable.id]] ?: Triple(0, 0, 0)
            row.toSaClub(mc, tc, mem)
        }
    }

    // SA-014: create club
    suspend fun createClub(request: CreateSaClubRequest, actorId: String): SaClub = newSuspendedTransaction {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val clubId = UUID.randomUUID()
        ClubsTable.insert {
            it[id] = clubId
            it[name] = request.name
            it[sportType] = request.sportType
            it[location] = request.location
            it[metadata] = request.metadata
            it[status] = "active"
            it[createdAt] = now
            it[updatedAt] = now
        }
        for (email in request.managerEmails) {
            createManagerInviteInternal(clubId.toString(), email, actorId)
        }
        val row = ClubsTable.selectAll().where { ClubsTable.id eq clubId }.single()
        val (mc, tc, mem) = fetchClubCounts(listOf(clubId))[clubId] ?: Triple(0, 0, 0)
        row.toSaClub(mc, tc, mem)
    }

    // SA-015: club detail
    suspend fun getClub(clubId: String): SaClub? = newSuspendedTransaction {
        val uid = UUID.fromString(clubId)
        val row = ClubsTable.selectAll().where {
            (ClubsTable.id eq uid) and ClubsTable.deletedAt.isNull()
        }.singleOrNull() ?: return@newSuspendedTransaction null
        val (mc, tc, mem) = fetchClubCounts(listOf(uid))[uid] ?: Triple(0, 0, 0)
        row.toSaClub(mc, tc, mem)
    }

    // SA-016: edit club
    suspend fun updateClub(clubId: String, request: UpdateSaClubRequest): SaClub = newSuspendedTransaction {
        val uid = UUID.fromString(clubId)
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        ClubsTable.update({ ClubsTable.id eq uid }) {
            request.name?.let { v -> it[name] = v }
            request.metadata?.let { v -> it[metadata] = v }
            it[updatedAt] = now
        }
        val row = ClubsTable.selectAll().where { ClubsTable.id eq uid }.single()
        val (mc, tc, mem) = fetchClubCounts(listOf(uid))[uid] ?: Triple(0, 0, 0)
        row.toSaClub(mc, tc, mem)
    }

    // SA-017: deactivate
    suspend fun deactivateClub(clubId: String) = newSuspendedTransaction {
        ClubsTable.update({ ClubsTable.id eq UUID.fromString(clubId) }) {
            it[status] = "inactive"
            it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
    }

    // SA-018: reactivate
    suspend fun reactivateClub(clubId: String) = newSuspendedTransaction {
        ClubsTable.update({ ClubsTable.id eq UUID.fromString(clubId) }) {
            it[status] = "active"
            it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
    }

    // SA-019: permanent delete — requires club to be inactive first (H-2 fix)
    suspend fun deleteClub(clubId: String, confirmName: String) = newSuspendedTransaction {
        val club = ClubsTable.selectAll().where { ClubsTable.id eq UUID.fromString(clubId) }.singleOrNull()
            ?: throw NotFoundException("Club not found")
        if (club[ClubsTable.status] != "inactive") {
            throw IllegalArgumentException("Club must be deactivated before deletion")
        }
        if (club[ClubsTable.name] != confirmName) {
            throw IllegalArgumentException("Club name does not match")
        }
        ClubsTable.deleteWhere { id eq UUID.fromString(clubId) }
    }

    // SA-020: list managers
    suspend fun listManagers(clubId: String): List<SaManager> = newSuspendedTransaction {
        ClubManagersTable
            .leftJoin(UsersTable, { ClubManagersTable.userId }, { UsersTable.id })
            .selectAll()
            .where { ClubManagersTable.clubId eq UUID.fromString(clubId) }
            .map { row ->
                SaManager(
                    id = row[ClubManagersTable.id].toString(),
                    clubId = row[ClubManagersTable.clubId].toString(),
                    userId = row[ClubManagersTable.userId]?.toString(),
                    invitedEmail = row[ClubManagersTable.invitedEmail],
                    displayName = if (row[ClubManagersTable.userId] != null) row.getOrNull(UsersTable.displayName) else null,
                    status = row[ClubManagersTable.status],
                    addedAt = row[ClubManagersTable.addedAt].toInstant().toKotlinInstant().toString(),
                    acceptedAt = row[ClubManagersTable.acceptedAt]?.toInstant()?.toKotlinInstant()?.toString(),
                )
            }
    }

    // SA-021: invite manager by email
    suspend fun inviteManager(clubId: String, email: String, actorId: String): SaManager =
        newSuspendedTransaction {
            val existing = ClubManagersTable.selectAll().where {
                (ClubManagersTable.clubId eq UUID.fromString(clubId)) and
                (ClubManagersTable.invitedEmail eq email)
            }.singleOrNull()
            if (existing != null) throw IllegalArgumentException("Already a manager of this club")
            createManagerInviteInternal(clubId, email, actorId)
        }

    /**
     * Creates a club_managers row and (if the user doesn't exist yet) fires an invite email.
     *
     * If [email] matches an existing user the row is immediately set to "active".
     * Otherwise status is "pending" and an invite email is sent via [emailScope] (fire-and-forget;
     * failures are logged to stderr — caller can retry by re-inviting).
     *
     * Must be called inside an open Exposed transaction.
     */
    private fun createManagerInviteInternal(clubId: String, email: String, actorId: String): SaManager {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val id = UUID.randomUUID()
        val token = generateToken()
        val existingUser = UsersTable.selectAll().where { UsersTable.email eq email }.singleOrNull()
        ClubManagersTable.insert {
            it[ClubManagersTable.id] = id
            it[ClubManagersTable.clubId] = UUID.fromString(clubId)
            it[ClubManagersTable.userId] = existingUser?.get(UsersTable.id)
            it[invitedEmail] = email
            it[status] = if (existingUser != null) "active" else "pending"
            it[addedBy] = UUID.fromString(actorId)
            it[addedAt] = now
            it[acceptedAt] = if (existingUser != null) now else null
        }
        val row = ClubManagersTable.selectAll().where { ClubManagersTable.id eq id }.single()
        // H-3 fix: fire-and-forget with scoped coroutine, logging failures
        if (existingUser == null) {
            val baseUrl = config.propertyOrNull("app.baseUrl")?.getString() ?: "https://app.playbook.example"
            val link = "$baseUrl/manager-invite/$token"
            val fromAddress = config.propertyOrNull("smtp.fromAddress")?.getString() ?: "noreply@playbook.app"
            val fromName = config.propertyOrNull("smtp.fromName")?.getString() ?: "Playbook"
            val clubName = ClubsTable.selectAll().where { ClubsTable.id eq UUID.fromString(clubId) }
                .singleOrNull()?.get(ClubsTable.name) ?: ""
            emailScope.launch {
                runCatching {
                    com.playbook.email.sendManagerInviteEmail(mailer, email, clubName, link, fromAddress, fromName)
                }.onFailure { e ->
                    // Best-effort; caller can resend
                    System.err.println("Failed to send manager invite email to $email: ${e.message}")
                }
            }
        }
        return SaManager(
            id = row[ClubManagersTable.id].toString(),
            clubId = row[ClubManagersTable.clubId].toString(),
            userId = row[ClubManagersTable.userId]?.toString(),
            invitedEmail = row[ClubManagersTable.invitedEmail],
            displayName = existingUser?.get(UsersTable.displayName),
            status = row[ClubManagersTable.status],
            addedAt = row[ClubManagersTable.addedAt].toInstant().toKotlinInstant().toString(),
            acceptedAt = row[ClubManagersTable.acceptedAt]?.toInstant()?.toKotlinInstant()?.toString(),
        )
    }

    // SA-022: remove manager
    suspend fun removeManager(clubId: String, managerId: String) = newSuspendedTransaction {
        ClubManagersTable.deleteWhere {
            (ClubManagersTable.id eq UUID.fromString(managerId)) and
            (ClubManagersTable.clubId eq UUID.fromString(clubId))
        }
    }

    // SA-023: start impersonation session (C-2 fix: deleted the dead startImpersonation() duplicate)
    suspend fun startImpersonationSession(
        saId: String,
        clubId: String,
        managerId: String,
    ): Pair<UUID, OffsetDateTime> = newSuspendedTransaction {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val expiresAt = now.plusHours(1)
        val sessionId = UUID.randomUUID()
        ImpersonationSessionsTable.insert {
            it[id] = sessionId
            it[superadminId] = UUID.fromString(saId)
            it[ImpersonationSessionsTable.managerId] = UUID.fromString(managerId)
            it[ImpersonationSessionsTable.clubId] = UUID.fromString(clubId)
            it[startedAt] = now
            it[ImpersonationSessionsTable.expiresAt] = expiresAt
        }
        Pair(sessionId, expiresAt)
    }

    // SA-024: end impersonation — H-5 fix: verify session belongs to calling SA
    suspend fun endImpersonation(sessionId: String, saId: String) = newSuspendedTransaction {
        val updated = ImpersonationSessionsTable.update({
            (ImpersonationSessionsTable.id eq UUID.fromString(sessionId)) and
            (ImpersonationSessionsTable.superadminId eq UUID.fromString(saId))
        }) {
            it[endedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        if (updated == 0) throw NotFoundException("Session not found or not owned by this account")
    }

    // SA-025: user search
    suspend fun searchUsers(q: String): List<SaUserSearchResult> = newSuspendedTransaction {
        val users = UsersTable.selectAll().where {
            (UsersTable.email.lowerCase() like "%${q.lowercase()}%") or
            (UsersTable.displayName.lowerCase() like "%${q.lowercase()}%")
        }.limit(50).toList()

        users.map { user ->
            val userId = user[UsersTable.id]
            val memberships = (ClubManagersTable innerJoin ClubsTable)
                .selectAll()
                .where {
                    (ClubManagersTable.userId eq userId) and
                    (ClubManagersTable.status eq "active")
                }
                .map { row ->
                    SaUserMembership(
                        clubId = row[ClubsTable.id].toString(),
                        clubName = row[ClubsTable.name],
                        role = "club_manager",
                    )
                }
            SaUserSearchResult(
                userId = userId.toString(),
                email = user[UsersTable.email],
                displayName = user[UsersTable.displayName],
                memberships = memberships,
            )
        }
    }

    // SA-026: audit log paginated
    suspend fun listAuditLog(
        actorId: String?,
        action: String?,
        from: String?,
        to: String?,
        page: Int,
        pageSize: Int,
    ): AuditLogPage = newSuspendedTransaction {
        var query = AuditLogTable.selectAll()
        actorId?.let { id -> query = query.andWhere { AuditLogTable.actorId eq UUID.fromString(id) } }
        action?.let { a -> query = query.andWhere { AuditLogTable.action like "%$a%" } }
        from?.let { f ->
            runCatching { OffsetDateTime.parse(f) }.getOrNull()?.let { dt ->
                query = query.andWhere { AuditLogTable.createdAt greaterEq dt }
            }
        }
        to?.let { t ->
            runCatching { OffsetDateTime.parse(t) }.getOrNull()?.let { dt ->
                query = query.andWhere { AuditLogTable.createdAt less dt }
            }
        }
        val total = query.count()
        val entries = query
            .orderBy(AuditLogTable.createdAt, SortOrder.DESC)
            .limit(pageSize, ((page - 1) * pageSize).toLong())
            .map { it.toAuditLogEntry() }
        AuditLogPage(entries, total, page, pageSize)
    }

    // SA-027: single audit log entry
    suspend fun getAuditLogEntry(id: String): AuditLogEntry? = newSuspendedTransaction {
        AuditLogTable.selectAll().where { AuditLogTable.id eq UUID.fromString(id) }
            .singleOrNull()?.toAuditLogEntry()
    }

    // SA-028: enqueue export job
    suspend fun enqueueExportJob(actorId: String, filters: String?): String = newSuspendedTransaction {
        val jobId = UUID.randomUUID()
        ExportJobsTable.insert {
            it[id] = jobId
            it[type] = "audit_log_csv"
            it[ExportJobsTable.actorId] = UUID.fromString(actorId)
            it[status] = "pending"
            it[ExportJobsTable.filters] = filters
            it[createdAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        jobId.toString()
    }

    // SA-029: poll export job status
    suspend fun getExportJob(jobId: String, baseUrl: String): ExportStatusResponse? = newSuspendedTransaction {
        ExportJobsTable.selectAll().where { ExportJobsTable.id eq UUID.fromString(jobId) }
            .singleOrNull()?.let { row ->
                val downloadUrl = if (row[ExportJobsTable.status] == "done" && row[ExportJobsTable.resultPath] != null) {
                    "$baseUrl/sa/audit-log/export/$jobId/download"
                } else null
                ExportStatusResponse(jobId, row[ExportJobsTable.status], downloadUrl)
            }
    }

    fun getExportFilePath(jobId: String): String? {
        val file = File(System.getProperty("user.dir"), "exports/$jobId.csv")
        return if (file.exists()) file.absolutePath else null
    }

    // SA-030: club members — distinct per user (no cross-team duplicates)
    suspend fun listClubMembers(clubId: String): List<SaClubMember> = newSuspendedTransaction {
        (TeamMembershipsTable innerJoin TeamsTable)
            .leftJoin(UsersTable, { TeamMembershipsTable.userId }, { UsersTable.id })
            .selectAll()
            .where { TeamsTable.clubId eq UUID.fromString(clubId) }
            .distinctBy { it[TeamMembershipsTable.userId] }
            .map { row ->
                SaClubMember(
                    userId = row[TeamMembershipsTable.userId].toString(),
                    email = row[UsersTable.email],
                    displayName = row[UsersTable.displayName],
                    role = row[TeamMembershipsTable.role],
                    joinedAt = row[TeamMembershipsTable.joinedAt].toInstant().toKotlinInstant().toString(),
                )
            }
    }

    // SA-031: billing summary — SQL COUNT DISTINCT (no in-memory distinctBy)
    suspend fun getBillingSummary(rateChf: Double): List<ClubBillingEntry> = newSuspendedTransaction {
        val clubs = ClubsTable.selectAll().where { ClubsTable.deletedAt.isNull() }.toList()
        val clubIds = clubs.map { it[ClubsTable.id] }
        val memberCountMap = if (clubIds.isEmpty()) emptyMap() else {
            (TeamMembershipsTable innerJoin TeamsTable)
                .select(TeamsTable.clubId, TeamMembershipsTable.userId.countDistinct())
                .where { TeamsTable.clubId inList clubIds }
                .groupBy(TeamsTable.clubId)
                .associate {
                    it[TeamsTable.clubId] to it[TeamMembershipsTable.userId.countDistinct()].toInt()
                }
        }
        clubs.map { club ->
            val count = memberCountMap[club[ClubsTable.id]] ?: 0
            ClubBillingEntry(
                clubId = club[ClubsTable.id].toString(),
                clubName = club[ClubsTable.name],
                activeMemberCount = count,
                annualBillingChf = count * rateChf,
            )
        }
    }

    // H-4 fix: batch counts for multiple clubs in 3 queries instead of 3×N
    private fun fetchClubCounts(clubIds: List<UUID>): Map<UUID, Triple<Int, Int, Int>> {
        if (clubIds.isEmpty()) return emptyMap()
        val managerCounts = ClubManagersTable
            .select(ClubManagersTable.clubId, ClubManagersTable.id.count())
            .where { (ClubManagersTable.clubId inList clubIds) and (ClubManagersTable.status eq "active") }
            .groupBy(ClubManagersTable.clubId)
            .associate { it[ClubManagersTable.clubId] to it[ClubManagersTable.id.count()].toInt() }
        val teamCounts = TeamsTable
            .select(TeamsTable.clubId, TeamsTable.id.count())
            .where { TeamsTable.clubId inList clubIds }
            .groupBy(TeamsTable.clubId)
            .associate { it[TeamsTable.clubId] to it[TeamsTable.id.count()].toInt() }
        val memberCounts = (TeamMembershipsTable innerJoin TeamsTable)
            .select(TeamsTable.clubId, TeamMembershipsTable.userId.countDistinct())
            .where { TeamsTable.clubId inList clubIds }
            .groupBy(TeamsTable.clubId)
            .associate { it[TeamsTable.clubId] to it[TeamMembershipsTable.userId.countDistinct()].toInt() }
        return clubIds.associateWith { id ->
            Triple(managerCounts[id] ?: 0, teamCounts[id] ?: 0, memberCounts[id] ?: 0)
        }
    }

    private fun ResultRow.toSaClub(managerCount: Int, teamCount: Int, memberCount: Int) = SaClub(
        id = this[ClubsTable.id].toString(),
        name = this[ClubsTable.name],
        status = this[ClubsTable.status],
        sportType = this[ClubsTable.sportType],
        location = this[ClubsTable.location],
        metadata = this[ClubsTable.metadata],
        createdAt = this[ClubsTable.createdAt].toInstant().toKotlinInstant().toString(),
        managerCount = managerCount,
        memberCount = memberCount,
        teamCount = teamCount,
    )

    private fun ResultRow.toAuditLogEntry() = AuditLogEntry(
        id = this[AuditLogTable.id].toString(),
        actorId = this[AuditLogTable.actorId].toString(),
        action = this[AuditLogTable.action],
        targetType = this[AuditLogTable.targetType],
        targetId = this[AuditLogTable.targetId]?.toString(),
        payload = this[AuditLogTable.payload],
        impersonatedAs = this[AuditLogTable.impersonatedAs]?.toString(),
        impersonationSessionId = this[AuditLogTable.impersonationSessionId]?.toString(),
        createdAt = this[AuditLogTable.createdAt].toInstant().toKotlinInstant().toString(),
    )
}
