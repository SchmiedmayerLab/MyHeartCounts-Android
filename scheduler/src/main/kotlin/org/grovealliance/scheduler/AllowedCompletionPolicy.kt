//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.scheduler

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Decides when an [Event] is allowed to be completed. The decision itself is applied by
 * [ScheduleCalculator], which resolves calendar-day boundaries against a fixed zone.
 */
@Serializable
enum class AllowedCompletionPolicy {
    /**
     * Completion is allowed while the event occurs today.
     */
    @SerialName("sameDay")
    SAME_DAY,

    /**
     * Completion is allowed once the event's start has passed.
     */
    @SerialName("afterStart")
    AFTER_START,

    /**
     * Completion is allowed after the event's start, while it is still occurring today.
     */
    @SerialName("sameDayAfterStart")
    SAME_DAY_AFTER_START,

    /**
     * Completion is allowed only while the event is occurring.
     */
    @SerialName("duringEvent")
    DURING_EVENT,

    /**
     * Completion is allowed at any time, before, during, or after the occurrence.
     */
    @SerialName("anytime")
    ANYTIME,
}
