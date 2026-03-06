package com.playbook.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

class AbwesenheitDomainTest : StringSpec({

    "AbwesenheitPresetType has expected values" {
        val names = AbwesenheitPresetType.entries.map { it.name }
        names shouldBe listOf("HOLIDAYS", "INJURY", "WORK", "SCHOOL", "TRAVEL", "OTHER")
    }

    "AbwesenheitRuleType has RECURRING and PERIOD values" {
        val names = AbwesenheitRuleType.entries.map { it.name }
        names shouldBe listOf("RECURRING", "PERIOD")
    }

    "AbwesenheitRule holds required fields" {
        val now = Instant.fromEpochSeconds(0)
        val rule = AbwesenheitRule(
            id = "r1",
            userId = "u1",
            presetType = AbwesenheitPresetType.WORK,
            label = "Monday work",
            ruleType = AbwesenheitRuleType.RECURRING,
            weekdays = listOf(1),
            createdAt = now,
            updatedAt = now,
        )
        rule.id shouldBe "r1"
        rule.userId shouldBe "u1"
        rule.presetType shouldBe AbwesenheitPresetType.WORK
        rule.ruleType shouldBe AbwesenheitRuleType.RECURRING
        rule.weekdays shouldBe listOf(1)
    }

    "AbwesenheitRule startDate and endDate can be null for recurring rules" {
        val now = Instant.fromEpochSeconds(0)
        val rule = AbwesenheitRule(
            id = "r2",
            userId = "u1",
            presetType = AbwesenheitPresetType.SCHOOL,
            label = "School",
            ruleType = AbwesenheitRuleType.RECURRING,
            weekdays = listOf(2, 4),
            createdAt = now,
            updatedAt = now,
        )
        rule.startDate shouldBe null
        rule.endDate shouldBe null
    }

    "AbwesenheitRule weekdays can be null for period rules" {
        val now = Instant.fromEpochSeconds(0)
        val start = LocalDate(2025, 1, 1)
        val end = LocalDate(2025, 1, 31)
        val rule = AbwesenheitRule(
            id = "r3",
            userId = "u1",
            presetType = AbwesenheitPresetType.HOLIDAYS,
            label = "Winter holiday",
            ruleType = AbwesenheitRuleType.PERIOD,
            startDate = start,
            endDate = end,
            createdAt = now,
            updatedAt = now,
        )
        rule.weekdays shouldBe null
        rule.startDate shouldBe start
        rule.endDate shouldBe end
    }

    "CreateAbwesenheitRuleRequest holds required fields with optional nulls" {
        val req = CreateAbwesenheitRuleRequest(
            presetType = AbwesenheitPresetType.INJURY,
            label = "Knee injury",
            ruleType = AbwesenheitRuleType.PERIOD,
            startDate = LocalDate(2025, 3, 1),
            endDate = LocalDate(2025, 3, 31),
        )
        req.presetType shouldBe AbwesenheitPresetType.INJURY
        req.label shouldBe "Knee injury"
        req.ruleType shouldBe AbwesenheitRuleType.PERIOD
        req.weekdays shouldBe null
    }

    "CreateAbwesenheitRuleRequest for recurring rule has weekdays" {
        val req = CreateAbwesenheitRuleRequest(
            presetType = AbwesenheitPresetType.WORK,
            label = "Work days",
            ruleType = AbwesenheitRuleType.RECURRING,
            weekdays = listOf(1, 2, 3, 4, 5),
        )
        req.weekdays shouldBe listOf(1, 2, 3, 4, 5)
        req.startDate shouldBe null
        req.endDate shouldBe null
    }
})
