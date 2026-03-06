package com.playbook.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Instant

class TeamDomainTest : StringSpec({

    "TeamStatus has expected values" {
        val names = TeamStatus.entries.map { it.name }
        names shouldBe listOf("ACTIVE", "ARCHIVED", "PENDING", "REJECTED")
    }

    "Team data class holds all fields" {
        val now = Instant.fromEpochSeconds(0)
        val team = Team(
            id = "t1",
            clubId = "c1",
            name = "FC Test",
            description = "A test team",
            status = TeamStatus.ACTIVE,
            requestedBy = null,
            rejectionReason = null,
            createdAt = now,
            updatedAt = now,
        )
        team.id shouldBe "t1"
        team.clubId shouldBe "c1"
        team.name shouldBe "FC Test"
        team.status shouldBe TeamStatus.ACTIVE
    }

    "Team description and requestedBy can be null" {
        val now = Instant.fromEpochSeconds(0)
        val team = Team(
            id = "t2",
            clubId = "c1",
            name = "Minimal",
            description = null,
            status = TeamStatus.PENDING,
            requestedBy = "u1",
            rejectionReason = null,
            createdAt = now,
            updatedAt = now,
        )
        team.description shouldBe null
        team.rejectionReason shouldBe null
    }

    "CreateTeamRequest holds name and optional description" {
        val req = CreateTeamRequest(name = "New Team")
        req.name shouldBe "New Team"
        req.description shouldBe null
    }

    "UpdateTeamRequest defaults to all nulls" {
        val req = UpdateTeamRequest()
        req.name shouldBe null
        req.description shouldBe null
    }

    "RejectTeamRequest holds optional reason" {
        val req = RejectTeamRequest(reason = "Duplicate")
        req.reason shouldBe "Duplicate"
    }
})
