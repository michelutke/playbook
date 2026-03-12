package com.playbook.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Instant

class MemberDomainTest : StringSpec({

    "MemberRole has COACH and PLAYER values" {
        val names = MemberRole.entries.map { it.name }
        names shouldBe listOf("COACH", "PLAYER")
    }

    "RosterMember holds required fields" {
        val now = Instant.fromEpochSeconds(0)
        val member = RosterMember(
            userId = "u1",
            displayName = "Alice",
            avatarUrl = null,
            roles = listOf(MemberRole.PLAYER),
            joinedAt = now,
        )
        member.userId shouldBe "u1"
        member.displayName shouldBe "Alice"
        member.roles shouldBe listOf(MemberRole.PLAYER)
    }

    "RosterMember avatarUrl and displayName can be null" {
        val now = Instant.fromEpochSeconds(0)
        val member = RosterMember(
            userId = "u2",
            displayName = null,
            avatarUrl = null,
            roles = listOf(MemberRole.COACH),
            joinedAt = now,
        )
        member.displayName shouldBe null
        member.avatarUrl shouldBe null
    }

    "TeamMembership holds all fields" {
        val now = Instant.fromEpochSeconds(0)
        val membership = TeamMembership(
            id = "m1",
            teamId = "t1",
            userId = "u1",
            role = MemberRole.COACH,
            addedBy = "admin",
            joinedAt = now,
        )
        membership.id shouldBe "m1"
        membership.teamId shouldBe "t1"
        membership.role shouldBe MemberRole.COACH
    }

    "TeamMembership addedBy can be null" {
        val now = Instant.fromEpochSeconds(0)
        val membership = TeamMembership(
            id = "m2",
            teamId = "t1",
            userId = "u2",
            role = MemberRole.PLAYER,
            addedBy = null,
            joinedAt = now,
        )
        membership.addedBy shouldBe null
    }

    "AddRoleRequest holds role" {
        val req = AddRoleRequest(role = MemberRole.PLAYER)
        req.role shouldBe MemberRole.PLAYER
    }
})
