package com.playbook.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SerializationRoundTripTest : StringSpec({

    val json = Json { ignoreUnknownKeys = true }
    val now = Instant.fromEpochSeconds(1_700_000_000)

    "Team serializes and deserializes correctly" {
        val team = Team(
            id = "t1",
            clubId = "c1",
            name = "FC Test",
            description = null,
            status = TeamStatus.ACTIVE,
            requestedBy = null,
            rejectionReason = null,
            createdAt = now,
            updatedAt = now,
        )
        val decoded = json.decodeFromString<Team>(json.encodeToString(team))
        decoded shouldBe team
    }

    "Club serializes and deserializes correctly" {
        val club = Club(
            id = "c1",
            name = "Test Club",
            logoUrl = null,
            sportType = "Football",
            location = "Berlin",
            status = ClubStatus.ACTIVE,
            createdAt = now,
            updatedAt = now,
        )
        val decoded = json.decodeFromString<Club>(json.encodeToString(club))
        decoded shouldBe club
    }

    "Event serializes and deserializes correctly" {
        val event = Event(
            id = "e1",
            title = "Training",
            type = EventType.TRAINING,
            startAt = now,
            endAt = now,
            status = EventStatus.ACTIVE,
            seriesOverride = false,
            createdBy = "u1",
            createdAt = now,
            updatedAt = now,
        )
        val decoded = json.decodeFromString<Event>(json.encodeToString(event))
        decoded shouldBe event
    }

    "AttendanceResponse serializes and deserializes correctly" {
        val response = AttendanceResponse(
            eventId = "e1",
            userId = "u1",
            status = AttendanceResponseStatus.CONFIRMED,
            updatedAt = now,
        )
        val decoded = json.decodeFromString<AttendanceResponse>(json.encodeToString(response))
        decoded shouldBe response
    }

    "AttendanceResponseStatus SerialName round-trips correctly" {
        val status = AttendanceResponseStatus.DECLINED_AUTO
        val encoded = json.encodeToString(status)
        encoded shouldBe "\"declined-auto\""
        val decoded = json.decodeFromString<AttendanceResponseStatus>(encoded)
        decoded shouldBe status
    }

    "AttendanceRecordStatus SerialName round-trips correctly" {
        val status = AttendanceRecordStatus.EXCUSED
        val encoded = json.encodeToString(status)
        encoded shouldBe "\"excused\""
        val decoded = json.decodeFromString<AttendanceRecordStatus>(encoded)
        decoded shouldBe status
    }

    "Notification serializes and deserializes correctly" {
        val notification = Notification(
            id = "n1",
            userId = "u1",
            type = "event_created",
            title = "New Event",
            body = "Training on Monday",
            deepLink = "playbook://events/e1",
            referenceId = "e1",
            read = false,
            createdAt = "2025-01-01T00:00:00Z",
        )
        val decoded = json.decodeFromString<Notification>(json.encodeToString(notification))
        decoded shouldBe notification
    }
})
