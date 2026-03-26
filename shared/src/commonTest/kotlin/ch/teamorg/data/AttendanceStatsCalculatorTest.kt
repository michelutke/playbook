package ch.teamorg.data

import ch.teamorg.domain.AttendanceResponse
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals

class AttendanceStatsCalculatorTest {

    private fun makeResponse(eventId: String, status: String) = AttendanceResponse(
        eventId = eventId,
        userId = "u1",
        status = status,
        reason = null,
        abwesenheitRuleId = null,
        manualOverride = false,
        respondedAt = null,
        updatedAt = Clock.System.now()
    )

    @Test
    fun `empty responses return zero stats`() {
        val stats = AttendanceStatsCalculator.calculateStats(emptyList(), emptyMap())
        assertEquals(0, stats.totalEvents)
        assertEquals(0f, stats.presencePct)
        assertEquals(0f, stats.trainingPresencePct)
        assertEquals(0f, stats.matchPresencePct)
    }

    @Test
    fun `all confirmed gives 100 percent`() {
        val responses = listOf(
            makeResponse("e1", "confirmed"),
            makeResponse("e2", "confirmed")
        )
        val types = mapOf("e1" to "training", "e2" to "match")
        val stats = AttendanceStatsCalculator.calculateStats(responses, types)
        assertEquals(1f, stats.presencePct)
        assertEquals(1f, stats.trainingPresencePct)
        assertEquals(1f, stats.matchPresencePct)
        assertEquals(2, stats.totalEvents)
        assertEquals(2, stats.presentCount)
    }

    @Test
    fun `mixed statuses calculate correctly`() {
        val responses = listOf(
            makeResponse("e1", "confirmed"),
            makeResponse("e2", "declined"),
            makeResponse("e3", "confirmed"),
            makeResponse("e4", "declined-auto")
        )
        val types = mapOf(
            "e1" to "training",
            "e2" to "training",
            "e3" to "match",
            "e4" to "match"
        )
        val stats = AttendanceStatsCalculator.calculateStats(responses, types)
        assertEquals(0.5f, stats.presencePct)
        assertEquals(0.5f, stats.trainingPresencePct)
        assertEquals(0.5f, stats.matchPresencePct)
        assertEquals(2, stats.presentCount)
        assertEquals(2, stats.absentCount)
    }

    @Test
    fun `filters by event type correctly`() {
        val responses = listOf(
            makeResponse("e1", "confirmed"),  // training
            makeResponse("e2", "declined")     // match
        )
        val types = mapOf("e1" to "training", "e2" to "match")
        val stats = AttendanceStatsCalculator.calculateStats(responses, types)
        assertEquals(1f, stats.trainingPresencePct)
        assertEquals(0f, stats.matchPresencePct)
        assertEquals(0.5f, stats.presencePct)
    }

    @Test
    fun `no training events gives zero training pct`() {
        val responses = listOf(
            makeResponse("e1", "confirmed"),
            makeResponse("e2", "declined")
        )
        val types = mapOf("e1" to "match", "e2" to "match")
        val stats = AttendanceStatsCalculator.calculateStats(responses, types)
        assertEquals(0f, stats.trainingPresencePct)
        assertEquals(0.5f, stats.matchPresencePct)
    }
}
