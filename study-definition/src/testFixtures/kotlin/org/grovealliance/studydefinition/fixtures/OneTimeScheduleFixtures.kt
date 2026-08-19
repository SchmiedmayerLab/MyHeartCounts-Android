//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.studydefinition.fixtures

import org.grovealliance.studydefinition.DateComponents
import org.grovealliance.studydefinition.OneTimeSchedule
import org.grovealliance.studydefinition.StudyLifecycleEvent
import org.grovealliance.studydefinition.Time

/**
 * Fixtures for [OneTimeSchedule]. [create] returns an [OneTimeSchedule.Event] anchored to enrollment.
 */
object OneTimeScheduleFixtures {
    fun create(): OneTimeSchedule = createEvent()

    fun createDate(date: DateComponents = DateComponentsFixtures.create()): OneTimeSchedule.Date =
        OneTimeSchedule.Date(date)

    fun createEvent(
        event: StudyLifecycleEvent = StudyLifecycleEventFixtures.create(),
        offsetInDays: Int = 0,
        time: Time? = null,
    ): OneTimeSchedule.Event = OneTimeSchedule.Event(
        event = event,
        offsetInDays = offsetInDays,
        time = time,
    )
}
