package ch.teamorg.domain.repositories

import ch.teamorg.db.tables.AbwesenheitRulesTable
import ch.teamorg.db.tables.PresetType
import ch.teamorg.db.tables.RuleType
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

class AbwesenheitRepositoryImpl : AbwesenheitRepository {

    override suspend fun listRules(userId: UUID): List<AbwesenheitRuleRow> = transaction {
        AbwesenheitRulesTable.selectAll()
            .where { AbwesenheitRulesTable.userId eq userId }
            .map(::rowToRule)
    }

    override suspend fun createRule(userId: UUID, rule: CreateAbwesenheitRule): AbwesenheitRuleRow = transaction {
        val id = AbwesenheitRulesTable.insert {
            it[AbwesenheitRulesTable.userId] = userId
            it[presetType] = PresetType.valueOf(rule.presetType)
            it[label] = rule.label
            it[bodyPart] = rule.bodyPart
            it[ruleType] = RuleType.valueOf(rule.ruleType)
            it[weekdays] = rule.weekdays
            it[startDate] = rule.startDate
            it[endDate] = rule.endDate
        } get AbwesenheitRulesTable.id

        AbwesenheitRulesTable.selectAll()
            .where { AbwesenheitRulesTable.id eq id }
            .map(::rowToRule)
            .single()
    }

    override suspend fun updateRule(ruleId: UUID, rule: UpdateAbwesenheitRule): AbwesenheitRuleRow = transaction {
        AbwesenheitRulesTable.update({ AbwesenheitRulesTable.id eq ruleId }) {
            rule.presetType?.let { v -> it[presetType] = PresetType.valueOf(v) }
            rule.label?.let { v -> it[label] = v }
            it[bodyPart] = rule.bodyPart
            rule.ruleType?.let { v -> it[ruleType] = RuleType.valueOf(v) }
            it[weekdays] = rule.weekdays
            it[startDate] = rule.startDate
            it[endDate] = rule.endDate
            it[updatedAt] = Instant.now()
        }
        AbwesenheitRulesTable.selectAll()
            .where { AbwesenheitRulesTable.id eq ruleId }
            .map(::rowToRule)
            .single()
    }

    override suspend fun deleteRule(ruleId: UUID): Unit = transaction {
        AbwesenheitRulesTable.deleteWhere { AbwesenheitRulesTable.id eq ruleId }
    }

    override suspend fun getRule(ruleId: UUID): AbwesenheitRuleRow? = transaction {
        AbwesenheitRulesTable.selectAll()
            .where { AbwesenheitRulesTable.id eq ruleId }
            .map(::rowToRule)
            .singleOrNull()
    }

    private fun rowToRule(row: ResultRow): AbwesenheitRuleRow = AbwesenheitRuleRow(
        id = row[AbwesenheitRulesTable.id],
        userId = row[AbwesenheitRulesTable.userId],
        presetType = row[AbwesenheitRulesTable.presetType].name,
        label = row[AbwesenheitRulesTable.label],
        bodyPart = row[AbwesenheitRulesTable.bodyPart],
        ruleType = row[AbwesenheitRulesTable.ruleType].name,
        weekdays = row[AbwesenheitRulesTable.weekdays]?.toList(),
        startDate = row[AbwesenheitRulesTable.startDate],
        endDate = row[AbwesenheitRulesTable.endDate],
        createdAt = row[AbwesenheitRulesTable.createdAt],
        updatedAt = row[AbwesenheitRulesTable.updatedAt]
    )
}
