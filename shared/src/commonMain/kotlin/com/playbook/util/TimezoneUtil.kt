package com.playbook.util

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * ES-026: Converts a UTC Instant to LocalDateTime in the device's current timezone.
 * Uses kotlin.time.Instant via kotlinx.datetime bridge — avoids the removed
 * kotlinx.datetime.Instant confusion in future stdlib versions.
 */
fun Instant.toLocalDateTimeInSystemZone(): LocalDateTime =
    toLocalDateTime(TimeZone.currentSystemDefault())

fun Instant.toLocalDateTimeInZone(timeZone: TimeZone): LocalDateTime =
    toLocalDateTime(timeZone)
