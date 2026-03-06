package com.playbook.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Instant

class AttendanceDomainTest : StringSpec({

    "AttendanceResponseStatus has expected values" {
        val names = AttendanceResponseStatus.entries.map { it.name }
        names shouldBe listOf("CONFIRMED", "DECLINED", "UNSURE", "DECLINED_AUTO", "NO_RESPONSE")
    }

    "AttendanceRecordStatus has expected values" {
        val names = AttendanceRecordStatus.entries.map { it.name }
        names shouldBe listOf("PRESENT", "ABSENT", "EXCUSED")
    }

    "AttendanceResponse holds required fields" {
        val now = Instant.fromEpochSeconds(0)
        val response = AttendanceResponse(
            eventId = "e1",
            userId = "u1",
            status = AttendanceResponseStatus.CONFIRMED,
            updatedAt = now,
        )
        response.eventId shouldBe "e1"
        response.userId shouldBe "u1"
        response.status shouldBe AttendanceResponseStatus.CONFIRMED
    }

    "AttendanceResponse optional fields default correctly" {
        val now = Instant.fromEpochSeconds(0)
        val response = AttendanceResponse(
            eventId = "e1",
            userId = "u1",
            status = AttendanceResponseStatus.NO_RESPONSE,
            updatedAt = now,
        )
        response.reason shouldBe null
        response.abwesenheitRuleId shouldBe null
        response.manualOverride shouldBe false
        response.respondedAt shouldBe null
    }

    "UpdateAttendanceRequest holds status and optional reason" {
        val req = UpdateAttendanceRequest(
            status = AttendanceResponseStatus.DECLINED,
            reason = "Sick",
        )
        req.status shouldBe AttendanceResponseStatus.DECLINED
        req.reason shouldBe "Sick"
    }

    "UpdateAttendanceRequest reason defaults to null" {
        val req = UpdateAttendanceRequest(status = AttendanceResponseStatus.CONFIRMED)
        req.reason shouldBe null
    }

    "AttendanceRecord holds required fields" {
        val now = Instant.fromEpochSeconds(0)
        val record = AttendanceRecord(
            eventId = "e1",
            userId = "u1",
            status = AttendanceRecordStatus.PRESENT,
            setBy = "coach1",
            setAt = now,
        )
        record.eventId shouldBe "e1"
        record.status shouldBe AttendanceRecordStatus.PRESENT
        record.setBy shouldBe "coach1"
        record.note shouldBe null
        record.previousStatus shouldBe null
    }
})
