//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition.fixtures

import edu.stanford.spezi.foundation.fixtures.UUIDFixtures
import edu.stanford.spezi.scheduler.AllowedCompletionPolicy
import edu.stanford.spezi.studydefinition.ComponentSchedule
import edu.stanford.spezi.studydefinition.NotificationsConfig
import edu.stanford.spezi.studydefinition.ScheduleDefinition
import java.util.UUID

/**
 * Fixture for [ComponentSchedule].
 */
object ComponentScheduleFixtures {
    fun create(
        id: UUID = UUIDFixtures.zero,
        componentId: UUID = UUIDFixtures.zero,
        scheduleDefinition: ScheduleDefinition = ScheduleDefinitionFixtures.create(),
        completionPolicy: AllowedCompletionPolicy = AllowedCompletionPolicy.SAME_DAY,
        notifications: NotificationsConfig = NotificationsConfigFixtures.create(),
    ): ComponentSchedule = ComponentSchedule(
        id = id,
        componentId = componentId,
        scheduleDefinition = scheduleDefinition,
        completionPolicy = completionPolicy,
        notifications = notifications,
    )
}
