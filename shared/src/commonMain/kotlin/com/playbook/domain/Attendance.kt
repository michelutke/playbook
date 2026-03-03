package com.playbook.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AttendanceResponseStatus {
    @SerialName("confirmed") CONFIRMED,
    @SerialName("declined") DECLINED,
    @SerialName("unsure") UNSURE,
    @SerialName("declined-auto") DECLINED_AUTO,
    @SerialName("no-response") NO_RESPONSE,
}

@Serializable
enum class AttendanceRecordStatus {
    @SerialName("present") PRESENT,
    @SerialName("absent") ABSENT,
    @SerialName("excused") EXCUSED,
}

@Serializable
data class AttendanceResponse(
    val eventId: String,
    val userId: String,
    val status: AttendanceResponseStatus,
    val reason: String? = null,
    val abwesenheitRuleId: String? = null,
    val manualOverride: Boolean = false,
    val respondedAt: Instant? = null,
    val updatedAt: Instant,
)

@Serializable
data class AttendanceRecord(
    val eventId: String,
    val userId: String,
    val status: AttendanceRecordStatus,
    val note: String? = null,
    val setBy: String,
    val setAt: Instant,
    val previousStatus: AttendanceRecordStatus? = null,
    val previousSetBy: String? = null,
)

@Serializable
data class AttendanceEntry(
    val userId: String,
    val displayName: String = "",
    val response: AttendanceResponse? = null,
    val record: AttendanceRecord? = null,
)

@Serializable
data class TeamAttendanceView(
    val confirmed: List<AttendanceEntry> = emptyList(),
    val declined: List<AttendanceEntry> = emptyList(),
    val unsure: List<AttendanceEntry> = emptyList(),
    val noResponse: List<AttendanceEntry> = emptyList(),
)

@Serializable
data class UpdateAttendanceRequest(
    val status: AttendanceResponseStatus,
    val reason: String? = null,
)

@Serializable
data class UpdateCheckInRequest(
    val status: AttendanceRecordStatus,
    val note: String? = null,
)

@Serializable
data class AttendanceRow(
    val eventId: String,
    val userId: String = "",
    val eventType: EventType,
    val eventDate: Instant,
    val response: AttendanceResponse? = null,
    val record: AttendanceRecord? = null,
)

@Serializable
data class AttendanceStats(
    val presencePct: Double,
    val trainingPct: Double,
    val matchPct: Double,
    val totalEvents: Int,
    val totalTraining: Int,
    val totalMatches: Int,
)
