//
// This source file is part of the My Heart Counts Android open-source project
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
    val limit: Int = DEFAULT_LIMIT,
    val window: Duration = Duration.ofDays(DEFAULT_WINDOW_DAYS),
    val allDayNotificationTime: NotificationTime = NotificationTime(hour = DEFAULT_ALL_DAY_HOUR),
) {
    init {
        require(limit > 0) { "limit must be positive" }
        require(!window.isNegative && !window.isZero) { "window must be positive" }
    }

    companion object {
        const val DEFAULT_LIMIT = 30
        const val DEFAULT_WINDOW_DAYS = 28L
        const val DEFAULT_ALL_DAY_HOUR = 9
    }
}
