//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.studydefinition.fixtures

import org.grovealliance.studydefinition.Time

/**
 * Fixture for [Time].
 */
object TimeFixtures {
    fun create(
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
    ): Time = Time(
        hour = hour,
        minute = minute,
        second = second,
    )
}
