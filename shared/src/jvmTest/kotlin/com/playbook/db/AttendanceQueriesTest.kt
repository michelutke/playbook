package com.playbook.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AttendanceQueriesTest {

    private lateinit var driver: app.cash.sqldelight.db.SqlDriver
    private lateinit var db: PlaybookDatabase

    @BeforeEach
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        PlaybookDatabase.Schema.create(driver)
        db = PlaybookDatabase(driver)
    }

    @AfterEach
    fun teardown() {
        driver.close()
    }

    // --- cached_attendance_responses ---

    @Test
    fun `upsertAttendanceResponse inserts and getMyAttendance returns row`() {
        db.attendanceQueries.upsertAttendanceResponse(
            eventId = "e1",
            userId = "u1",
            status = "confirmed",
            reason = null,
            abwesenheitRuleId = null,
            manualOverride = 0,
            respondedAt = null,
            updatedAt = "2025-01-01T00:00:00Z",
        )
        val row = db.attendanceQueries.getMyAttendance("e1", "u1").executeAsOneOrNull()
        assertNotNull(row)
        assertEquals("confirmed", row.status)
        assertEquals("e1", row.event_id)
        assertEquals("u1", row.user_id)
    }

    @Test
    fun `upsertAttendanceResponse replaces existing row`() {
        db.attendanceQueries.upsertAttendanceResponse(
            eventId = "e1", userId = "u1", status = "confirmed",
            reason = null, abwesenheitRuleId = null, manualOverride = 0,
            respondedAt = null, updatedAt = "2025-01-01T00:00:00Z",
        )
        db.attendanceQueries.upsertAttendanceResponse(
            eventId = "e1", userId = "u1", status = "declined",
            reason = "Sick", abwesenheitRuleId = null, manualOverride = 0,
            respondedAt = null, updatedAt = "2025-01-02T00:00:00Z",
        )
        val row = db.attendanceQueries.getMyAttendance("e1", "u1").executeAsOneOrNull()
        assertNotNull(row)
        assertEquals("declined", row.status)
        assertEquals("Sick", row.reason)
    }

    @Test
    fun `getMyAttendance returns null when no row exists`() {
        val row = db.attendanceQueries.getMyAttendance("e99", "u99").executeAsOneOrNull()
        assertNull(row)
    }

    @Test
    fun `deleteAttendanceResponse removes row`() {
        db.attendanceQueries.upsertAttendanceResponse(
            eventId = "e1", userId = "u1", status = "confirmed",
            reason = null, abwesenheitRuleId = null, manualOverride = 0,
            respondedAt = null, updatedAt = "2025-01-01T00:00:00Z",
        )
        db.attendanceQueries.deleteAttendanceResponse("e1", "u1")
        val row = db.attendanceQueries.getMyAttendance("e1", "u1").executeAsOneOrNull()
        assertNull(row)
    }

    // --- cached_abwesenheit_rules ---

    @Test
    fun `upsertAbwesenheitRule inserts and getRulesForUser returns row`() {
        db.attendanceQueries.upsertAbwesenheitRule(
            id = "r1",
            userId = "u1",
            presetType = "WORK",
            label = "Monday work",
            ruleType = "RECURRING",
            weekdays = "1",
            startDate = null,
            endDate = null,
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z",
        )
        val rules = db.attendanceQueries.getRulesForUser("u1").executeAsList()
        assertEquals(1, rules.size)
        assertEquals("r1", rules.first().id)
        assertEquals("WORK", rules.first().preset_type)
    }

    @Test
    fun `getRulesForUser returns only rules for requested user`() {
        db.attendanceQueries.upsertAbwesenheitRule(
            id = "r1", userId = "u1", presetType = "WORK", label = "L",
            ruleType = "RECURRING", weekdays = "1", startDate = null, endDate = null,
            createdAt = "2025-01-01T00:00:00Z", updatedAt = "2025-01-01T00:00:00Z",
        )
        db.attendanceQueries.upsertAbwesenheitRule(
            id = "r2", userId = "u2", presetType = "SCHOOL", label = "L",
            ruleType = "RECURRING", weekdays = "2", startDate = null, endDate = null,
            createdAt = "2025-01-01T00:00:00Z", updatedAt = "2025-01-01T00:00:00Z",
        )
        val rules = db.attendanceQueries.getRulesForUser("u1").executeAsList()
        assertEquals(1, rules.size)
        assertEquals("u1", rules.first().user_id)
    }

    @Test
    fun `deleteAbwesenheitRule removes row`() {
        db.attendanceQueries.upsertAbwesenheitRule(
            id = "r1", userId = "u1", presetType = "WORK", label = "L",
            ruleType = "RECURRING", weekdays = "1", startDate = null, endDate = null,
            createdAt = "2025-01-01T00:00:00Z", updatedAt = "2025-01-01T00:00:00Z",
        )
        db.attendanceQueries.deleteAbwesenheitRule("r1")
        val rules = db.attendanceQueries.getRulesForUser("u1").executeAsList()
        assertTrue(rules.isEmpty())
    }

    // --- attendance_mutation_queue ---

    @Test
    fun `insertMutation adds to queue and getPendingMutations returns it`() {
        db.attendanceQueries.insertMutation(
            id = "m1",
            type = "update_response",
            payload = """{"eventId":"e1"}""",
            createdAt = "2025-01-01T00:00:00Z",
        )
        val mutations = db.attendanceQueries.getPendingMutations().executeAsList()
        assertEquals(1, mutations.size)
        assertEquals("m1", mutations.first().id)
        assertEquals("update_response", mutations.first().type)
    }

    @Test
    fun `deleteMutation removes specific mutation`() {
        db.attendanceQueries.insertMutation("m1", "type_a", "{}", "2025-01-01T00:00:00Z")
        db.attendanceQueries.insertMutation("m2", "type_b", "{}", "2025-01-02T00:00:00Z")
        db.attendanceQueries.deleteMutation("m1")
        val mutations = db.attendanceQueries.getPendingMutations().executeAsList()
        assertEquals(1, mutations.size)
        assertEquals("m2", mutations.first().id)
    }

    @Test
    fun `clearMutations empties the queue`() {
        db.attendanceQueries.insertMutation("m1", "type_a", "{}", "2025-01-01T00:00:00Z")
        db.attendanceQueries.insertMutation("m2", "type_b", "{}", "2025-01-02T00:00:00Z")
        db.attendanceQueries.clearMutations()
        val mutations = db.attendanceQueries.getPendingMutations().executeAsList()
        assertTrue(mutations.isEmpty())
    }

    @Test
    fun `getPendingMutations returns results ordered by createdAt ascending`() {
        db.attendanceQueries.insertMutation("m2", "type", "{}", "2025-01-02T00:00:00Z")
        db.attendanceQueries.insertMutation("m1", "type", "{}", "2025-01-01T00:00:00Z")
        val mutations = db.attendanceQueries.getPendingMutations().executeAsList()
        assertEquals("m1", mutations[0].id)
        assertEquals("m2", mutations[1].id)
    }
}
