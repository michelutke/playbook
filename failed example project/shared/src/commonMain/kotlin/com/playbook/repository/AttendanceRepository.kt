package com.playbook.repository

import com.playbook.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

interface AttendanceRepository {
    suspend fun getEventAttendance(eventId: String): TeamAttendanceView
    suspend fun getMyAttendance(eventId: String, userId: String): AttendanceResponse
    suspend fun upsertAttendance(eventId: String, userId: String, request: UpdateAttendanceRequest): AttendanceResponse
    fun observeMyAttendance(eventId: String): Flow<AttendanceResponse>
    suspend fun getCheckInList(eventId: String): List<AttendanceEntry>
    suspend fun setCheckIn(eventId: String, userId: String, request: UpdateCheckInRequest): AttendanceRecord
    suspend fun getUserAttendance(userId: String, from: Instant? = null, to: Instant? = null): List<AttendanceRow>
    suspend fun getTeamAttendance(teamId: String, from: Instant? = null, to: Instant? = null): List<AttendanceRow>
    suspend fun triggerBackfillForEvent(eventId: String, teamId: String)
}
