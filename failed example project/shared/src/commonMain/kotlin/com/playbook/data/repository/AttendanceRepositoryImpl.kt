package com.playbook.data.repository

import com.playbook.attendance.MutationQueue
import com.playbook.data.network.ApiConfig
import com.playbook.db.AttendanceQueries
import com.playbook.domain.*
import com.playbook.repository.AttendanceRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AttendanceRepositoryImpl(
    private val client: HttpClient,
    private val config: ApiConfig,
    private val queries: AttendanceQueries,
    private val mutationQueue: MutationQueue,
) : AttendanceRepository {

    override suspend fun getEventAttendance(eventId: String): TeamAttendanceView =
        client.get("${config.baseUrl}/events/$eventId/attendance") {
            bearerAuth(config.authTokenProvider() ?: "")
        }.body()

    override suspend fun getMyAttendance(eventId: String, userId: String): AttendanceResponse =
        client.get("${config.baseUrl}/events/$eventId/attendance/me") {
            bearerAuth(config.authTokenProvider() ?: "")
        }.body()

    override suspend fun upsertAttendance(eventId: String, userId: String, request: UpdateAttendanceRequest): AttendanceResponse {
        val resolvedUserId = userId.ifEmpty { config.userIdProvider?.invoke() ?: "" }
        val now = Clock.System.now()

        // Optimistic local cache write
        queries.upsertAttendanceResponse(
            eventId = eventId,
            userId = resolvedUserId,
            status = request.status.name,
            reason = request.reason,
            abwesenheitRuleId = null,
            manualOverride = 0L,
            respondedAt = now.toString(),
            updatedAt = now.toString(),
        )

        val mutationId = mutationQueue.enqueue(
            type = "upsert_attendance",
            payload = Json.encodeToString(
                UpsertAttendanceMutationPayload(eventId = eventId, request = request)
            ),
        )

        return try {
            val response: AttendanceResponse = client.put("${config.baseUrl}/events/$eventId/attendance/me") {
                bearerAuth(config.authTokenProvider() ?: "")
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

            // Network succeeded — remove from queue, refresh cache with server response
            mutationQueue.dequeue(mutationId)
            queries.upsertAttendanceResponse(
                eventId = response.eventId,
                userId = response.userId,
                status = response.status.name,
                reason = response.reason,
                abwesenheitRuleId = response.abwesenheitRuleId,
                manualOverride = if (response.manualOverride) 1L else 0L,
                respondedAt = response.respondedAt?.toString(),
                updatedAt = response.updatedAt.toString(),
            )

            response
        } catch (e: Exception) {
            // Network failed — revert optimistic write and remove the pending mutation
            queries.deleteAttendanceResponse(eventId = eventId, userId = resolvedUserId)
            mutationQueue.dequeue(mutationId)
            throw e
        }
    }

    override fun observeMyAttendance(eventId: String): Flow<AttendanceResponse> = flow {
        emit(getMyAttendance(eventId, userId = ""))
    }

    override suspend fun triggerBackfillForEvent(eventId: String, teamId: String) {
        // No-op on client — server handles backfill
    }

    override suspend fun getCheckInList(eventId: String): List<AttendanceEntry> =
        client.get("${config.baseUrl}/events/$eventId/check-in") {
            bearerAuth(config.authTokenProvider() ?: "")
        }.body()

    override suspend fun setCheckIn(eventId: String, userId: String, request: UpdateCheckInRequest): AttendanceRecord =
        client.put("${config.baseUrl}/events/$eventId/check-in/$userId") {
            bearerAuth(config.authTokenProvider() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun getUserAttendance(userId: String, from: Instant?, to: Instant?): List<AttendanceRow> =
        client.get("${config.baseUrl}/users/$userId/attendance") {
            bearerAuth(config.authTokenProvider() ?: "")
            from?.let { parameter("from", it.toString()) }
            to?.let { parameter("to", it.toString()) }
        }.body()

    override suspend fun getTeamAttendance(teamId: String, from: Instant?, to: Instant?): List<AttendanceRow> =
        client.get("${config.baseUrl}/teams/$teamId/attendance") {
            bearerAuth(config.authTokenProvider() ?: "")
            from?.let { parameter("from", it.toString()) }
            to?.let { parameter("to", it.toString()) }
        }.body()

    /**
     * Flushes all pending mutations to the network.
     * TODO: call this from a platform-specific connectivity observer
     * (e.g. Android ConnectivityManager.NetworkCallback, iOS NWPathMonitor)
     * when the device transitions from offline -> online.
     */
    suspend fun flush() {
        for (mutation in mutationQueue.getAll()) {
            if (mutation.type == "upsert_attendance") {
                try {
                    val payload = Json.decodeFromString<UpsertAttendanceMutationPayload>(mutation.payload)
                    client.put("${config.baseUrl}/events/${payload.eventId}/attendance/me") {
                        bearerAuth(config.authTokenProvider() ?: "")
                        contentType(ContentType.Application.Json)
                        setBody(payload.request)
                    }
                    mutationQueue.dequeue(mutation.id)
                } catch (_: Exception) {
                    // Leave in queue; will retry on next flush
                }
            }
        }
    }
}

@kotlinx.serialization.Serializable
private data class UpsertAttendanceMutationPayload(
    val eventId: String,
    val request: UpdateAttendanceRequest,
)
