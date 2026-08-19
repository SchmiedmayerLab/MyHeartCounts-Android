//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.studydefinition.fixtures

import org.grovealliance.studydefinition.RepetitionPattern
import org.grovealliance.studydefinition.Weekday

/**
 * Fixtures for [RepetitionPattern]. [create] returns a [RepetitionPattern.Daily].
 */
object RepetitionPatternFixtures {
    fun create(): RepetitionPattern = createDaily()

    fun createDaily(
        interval: Int = 0,
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
    ): RepetitionPattern.Daily = RepetitionPattern.Daily(
        interval = interval,
        hour = hour,
        minute = minute,
        second = second,
    )

    fun createWeekly(
        interval: Int = 0,
        weekday: Weekday? = null,
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
    ): RepetitionPattern.Weekly = RepetitionPattern.Weekly(
        interval = interval,
        weekday = weekday,
        hour = hour,
        minute = minute,
        second = second,
    )

    fun createMonthly(
        interval: Int = 0,
        day: Int? = null,
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
    ): RepetitionPattern.Monthly = RepetitionPattern.Monthly(
        interval = interval,
        day = day,
        hour = hour,
        minute = minute,
        second = second,
    )
}
