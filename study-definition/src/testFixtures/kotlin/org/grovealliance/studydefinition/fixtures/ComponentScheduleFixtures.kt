//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.studydefinition.fixtures

import org.grovealliance.foundation.fixtures.UUIDFixtures
import org.grovealliance.scheduler.AllowedCompletionPolicy
import org.grovealliance.studydefinition.ComponentSchedule
import org.grovealliance.studydefinition.NotificationsConfig
import org.grovealliance.studydefinition.ScheduleDefinition
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
