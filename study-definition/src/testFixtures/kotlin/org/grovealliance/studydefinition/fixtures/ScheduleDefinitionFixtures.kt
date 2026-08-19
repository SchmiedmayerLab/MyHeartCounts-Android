//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.studydefinition.fixtures

import org.grovealliance.studydefinition.DateComponents
import org.grovealliance.studydefinition.OneTimeSchedule
import org.grovealliance.studydefinition.RepetitionPattern
import org.grovealliance.studydefinition.ScheduleDefinition

/**
 * Fixtures for [ScheduleDefinition]. [create] returns a daily [ScheduleDefinition.Repeated].
 */
object ScheduleDefinitionFixtures {
    fun create(): ScheduleDefinition = createRepeated()

    fun createOnce(schedule: OneTimeSchedule = OneTimeScheduleFixtures.create()): ScheduleDefinition.Once =
        ScheduleDefinition.Once(schedule)

    fun createRepeated(
        pattern: RepetitionPattern = RepetitionPatternFixtures.create(),
        offset: DateComponents = DateComponentsFixtures.create(),
    ): ScheduleDefinition.Repeated = ScheduleDefinition.Repeated(
        pattern = pattern,
        offset = offset,
    )
}
