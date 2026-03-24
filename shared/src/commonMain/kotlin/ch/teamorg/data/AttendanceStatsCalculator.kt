package ch.teamorg.data

import ch.teamorg.domain.AttendanceResponse
import ch.teamorg.domain.AttendanceStats

object AttendanceStatsCalculator {
    fun calculateStats(
        responses: List<AttendanceResponse>,
        eventTypes: Map<String, String> // eventId -> "training"|"match"|"other"
    ): AttendanceStats {
        val total = responses.size
        if (total == 0) return AttendanceStats(0, 0, 0, 0, 0f, 0f, 0f)

        val present = responses.count { it.status == "confirmed" || it.status == "present" }
        val absent = responses.count {
            it.status == "declined" || it.status == "declined-auto" || it.status == "absent"
        }
        val excused = responses.count { it.status == "excused" }

        val trainingResponses = responses.filter { eventTypes[it.eventId] == "training" }
        val matchResponses = responses.filter { eventTypes[it.eventId] == "match" }

        val trainingPresent = trainingResponses.count { it.status == "confirmed" || it.status == "present" }
        val matchPresent = matchResponses.count { it.status == "confirmed" || it.status == "present" }

        return AttendanceStats(
            totalEvents = total,
            presentCount = present,
            absentCount = absent,
            excusedCount = excused,
            presencePct = present.toFloat() / total,
            trainingPresencePct = if (trainingResponses.isNotEmpty()) trainingPresent.toFloat() / trainingResponses.size else 0f,
            matchPresencePct = if (matchResponses.isNotEmpty()) matchPresent.toFloat() / matchResponses.size else 0f
        )
    }
}
