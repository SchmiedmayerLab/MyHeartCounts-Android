//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition.fixtures

import edu.stanford.spezi.studydefinition.DateComponents
import edu.stanford.spezi.studydefinition.OneTimeSchedule
import edu.stanford.spezi.studydefinition.RepetitionPattern
import edu.stanford.spezi.studydefinition.ScheduleDefinition

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
