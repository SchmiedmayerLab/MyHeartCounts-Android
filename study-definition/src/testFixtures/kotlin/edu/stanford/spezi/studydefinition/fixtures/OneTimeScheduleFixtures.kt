//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition.fixtures

import edu.stanford.spezi.studydefinition.DateComponents
import edu.stanford.spezi.studydefinition.OneTimeSchedule
import edu.stanford.spezi.studydefinition.StudyLifecycleEvent
import edu.stanford.spezi.studydefinition.Time

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
