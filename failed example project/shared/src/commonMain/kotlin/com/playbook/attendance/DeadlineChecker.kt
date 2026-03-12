package com.playbook.attendance

import com.playbook.domain.Event
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * T-024: Pure function to check if an event's response deadline has passed.
 * The deadline is the event's start time (players can't respond after event starts).
 */
fun isDeadlinePassed(event: Event): Boolean =
    Clock.System.now() >= event.startAt

fun isDeadlinePassed(deadline: Instant): Boolean =
    Clock.System.now() >= deadline
