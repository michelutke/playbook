package com.playbook.domain.sa

import kotlinx.serialization.Serializable

@Serializable
data class SaStats(
    val totalClubs: Int,
    val totalUsers: Int,
    val activeEventsToday: Int,
    val signUpsLast7Days: Int,
)

/**
 * Club summary returned by SA club list/detail endpoints.
 * [status]: "active" | "inactive" (soft-deactivated, not yet deleted)
 * [metadata]: free-form JSON string; null if not set.
 */
@Serializable
data class SaClub(
    val id: String,
    val name: String,
    val status: String,
    val sportType: String,
    val location: String?,
    val metadata: String?,
    val createdAt: String,
    val managerCount: Int,
    val memberCount: Int,
    val teamCount: Int,
)

/**
 * Club manager row (from club_managers table).
 * [userId]: null while invite is pending (user hasn't registered yet).
 * [status]: "pending" (invite sent, not accepted) | "active" (accepted or pre-existing user).
 * [acceptedAt]: null while still pending.
 */
@Serializable
data class SaManager(
    val id: String,
    val clubId: String,
    val userId: String?,
    val invitedEmail: String,
    val displayName: String?,
    val status: String,
    val addedAt: String,
    val acceptedAt: String?,
)

@Serializable
data class CreateSaClubRequest(
    val name: String,
    val sportType: String = "",
    val location: String? = null,
    val metadata: String? = null,
    val managerEmails: List<String> = emptyList(),
)

@Serializable
data class UpdateSaClubRequest(
    val name: String? = null,
    val metadata: String? = null,
)

@Serializable
data class InviteManagerRequest(
    val email: String,
)

/** Requires club name confirmation to prevent accidental permanent deletion. */
@Serializable
data class DeleteClubRequest(
    val confirmName: String,
)

@Serializable
data class ImpersonationResponse(
    val token: String,
    val sessionId: String,
    val expiresAt: String,
)

@Serializable
data class AuditLogEntry(
    val id: String,
    val actorId: String,
    val action: String,
    val targetType: String?,
    val targetId: String?,
    val payload: String?,
    val impersonatedAs: String?,
    val impersonationSessionId: String?,
    val createdAt: String,
)

@Serializable
data class AuditLogPage(
    val entries: List<AuditLogEntry>,
    val total: Long,
    val page: Int,
    val pageSize: Int,
)

@Serializable
data class ExportJobResponse(
    val jobId: String,
)

/**
 * [status]: "pending" | "processing" | "done" | "failed"
 * [downloadUrl]: non-null only when status == "done"; points to GET /sa/audit-log/export/{id}/download
 */
@Serializable
data class ExportStatusResponse(
    val jobId: String,
    val status: String,
    val downloadUrl: String?,
)

@Serializable
data class ClubBillingEntry(
    val clubId: String,
    val clubName: String,
    val activeMemberCount: Int,
    val annualBillingChf: Double,
)

@Serializable
data class SaUserSearchResult(
    val userId: String,
    val email: String,
    val displayName: String?,
    val memberships: List<SaUserMembership>,
)

@Serializable
data class SaUserMembership(
    val clubId: String,
    val clubName: String,
    val role: String,
)

@Serializable
data class SaClubMember(
    val userId: String,
    val email: String,
    val displayName: String?,
    val role: String,
    val joinedAt: String,
)
