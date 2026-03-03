package com.playbook.attendance

import com.playbook.domain.*

/**
 * T-025: Client-side stats aggregation from raw attendance rows.
 * Pure function, no network calls.
 */
data class StatsFilter(
    val eventType: EventType? = null, // null = all types
    val userId: String? = null,       // null = aggregate all users
)

fun aggregateStats(rows: List<AttendanceRow>, filter: StatsFilter = StatsFilter()): AttendanceStats {
    val filtered = rows.filter { row ->
        filter.eventType == null || row.eventType == filter.eventType
    }

    val total = filtered.size
    val trainings = filtered.filter { it.eventType == EventType.TRAINING }
    val matches = filtered.filter { it.eventType == EventType.MATCH }

    fun List<AttendanceRow>.presentCount() =
        count { it.record?.status == AttendanceRecordStatus.PRESENT }

    fun safePercent(count: Int, total: Int): Double =
        if (total == 0) 0.0 else count.toDouble() / total.toDouble() * 100.0

    return AttendanceStats(
        presencePct = safePercent(filtered.presentCount(), total),
        trainingPct = safePercent(trainings.presentCount(), trainings.size),
        matchPct = safePercent(matches.presentCount(), matches.size),
        totalEvents = total,
        totalTraining = trainings.size,
        totalMatches = matches.size,
    )
}
