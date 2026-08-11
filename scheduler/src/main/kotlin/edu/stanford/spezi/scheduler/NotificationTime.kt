//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.scheduler

import java.time.Instant
import java.time.ZoneId

/**
 * A time of day at which a task notification should be delivered.
 *
 * Used to place an all-day event's notification at a sensible hour, or to override the delivery time
 * of a timed event.
 */
data class NotificationTime(
    val hour: Int,
    val minute: Int = 0,
    val second: Int = 0,
) {
    init {
        require(hour in HOUR_RANGE) { "hour must be in $HOUR_RANGE" }
        require(minute in MINUTE_SECOND_RANGE) { "minute must be in $MINUTE_SECOND_RANGE" }
        require(second in MINUTE_SECOND_RANGE) { "second must be in $MINUTE_SECOND_RANGE" }
    }

    /**
     * The [Instant] at this time of day on the calendar day containing [day], interpreted in [zone].
     */
    fun onDayOf(day: Instant, zone: ZoneId): Instant =
        day.atZone(zone).toLocalDate().atTime(hour, minute, second).atZone(zone).toInstant()

    private companion object {
        val HOUR_RANGE = 0..23
        val MINUTE_SECOND_RANGE = 0..59
    }
}
