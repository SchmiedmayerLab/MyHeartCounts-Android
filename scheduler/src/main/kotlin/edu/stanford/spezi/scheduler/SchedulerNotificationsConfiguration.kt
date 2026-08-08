//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.scheduler

import java.time.Duration

/**
 * Tuning for the automatic scheduling of task notifications.
 *
 * Only events within [window] of now are pre-scheduled, at most [limit] at a time; all-day events are
 * delivered at [allDayNotificationTime].
 */
data class SchedulerNotificationsConfiguration(
    val limit: Int,
    val window: Duration,
    val allDayNotificationTime: NotificationTime,
) {
    init {
        require(limit > 0) { "limit must be positive" }
        require(!window.isNegative && !window.isZero) { "window must be positive" }
    }

    companion object {
        private const val DEFAULT_LIMIT = 30
        private const val DEFAULT_WINDOW_DAYS = 28L
        private const val DEFAULT_ALL_DAY_HOUR = 9

        val DEFAULT = SchedulerNotificationsConfiguration(
            limit = DEFAULT_LIMIT,
            window = Duration.ofDays(DEFAULT_WINDOW_DAYS),
            allDayNotificationTime = NotificationTime(hour = DEFAULT_ALL_DAY_HOUR)
        )
    }
}
