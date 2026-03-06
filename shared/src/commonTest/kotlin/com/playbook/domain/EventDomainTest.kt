package com.playbook.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Instant

class EventDomainTest : StringSpec({

    "EventType has TRAINING, MATCH, OTHER values" {
        val names = EventType.entries.map { it.name }
        names shouldBe listOf("TRAINING", "MATCH", "OTHER")
    }

    "EventStatus has ACTIVE and CANCELLED values" {
        val names = EventStatus.entries.map { it.name }
        names shouldBe listOf("ACTIVE", "CANCELLED")
    }

    "Event data class holds required fields" {
        val now = Instant.fromEpochSeconds(1_000_000)
        val event = Event(
            id = "e1",
            title = "Training Session",
            type = EventType.TRAINING,
            startAt = now,
            endAt = now,
            status = EventStatus.ACTIVE,
            seriesOverride = false,
            createdBy = "u1",
            createdAt = now,
            updatedAt = now,
        )
        event.id shouldBe "e1"
        event.title shouldBe "Training Session"
        event.type shouldBe EventType.TRAINING
        event.status shouldBe EventStatus.ACTIVE
        event.createdBy shouldBe "u1"
    }

    "Event optional fields default correctly" {
        val now = Instant.fromEpochSeconds(0)
        val event = Event(
            id = "e2",
            title = "Match",
            type = EventType.MATCH,
            startAt = now,
            endAt = now,
            status = EventStatus.ACTIVE,
            seriesOverride = false,
            createdBy = "u1",
            createdAt = now,
            updatedAt = now,
        )
        event.meetupAt shouldBe null
        event.location shouldBe null
        event.description shouldBe null
        event.cancelledAt shouldBe null
        event.seriesId shouldBe null
        event.teams shouldBe emptyList()
        event.subgroups shouldBe emptyList()
    }

    "CreateEventRequest holds title, type, teamIds, startAt, endAt" {
        val now = Instant.fromEpochSeconds(0)
        val req = CreateEventRequest(
            title = "Weekend Training",
            type = EventType.TRAINING,
            startAt = now,
            endAt = now,
            teamIds = listOf("t1", "t2"),
        )
        req.title shouldBe "Weekend Training"
        req.type shouldBe EventType.TRAINING
        req.teamIds shouldBe listOf("t1", "t2")
        req.subgroupIds shouldBe emptyList()
        req.recurring shouldBe null
    }
})
