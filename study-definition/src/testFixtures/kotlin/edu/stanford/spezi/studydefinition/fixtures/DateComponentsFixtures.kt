//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition.fixtures

import edu.stanford.spezi.studydefinition.DateComponents

/**
 * Fixture for [DateComponents].
 */
object DateComponentsFixtures {
    fun create(
        year: Int = 0,
        month: Int = 0,
        day: Int = 0,
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
    ): DateComponents = DateComponents(
        year = year,
        month = month,
        day = day,
        hour = hour,
        minute = minute,
        second = second,
    )
}
