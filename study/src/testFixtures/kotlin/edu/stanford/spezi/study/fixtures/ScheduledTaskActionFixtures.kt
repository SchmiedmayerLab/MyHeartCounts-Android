//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.study.fixtures

import edu.stanford.spezi.study.ScheduledTaskAction
import edu.stanford.spezi.studydefinition.Component
import edu.stanford.spezi.studydefinition.fixtures.ComponentFixtures

/**
 * Fixtures for [ScheduledTaskAction]. [create] returns a [ScheduledTaskAction.PresentInformational].
 */
object ScheduledTaskActionFixtures {
    fun create(): ScheduledTaskAction = createPresentInformational()

    fun createPresentInformational(
        component: Component.Informational = ComponentFixtures.createInformational(),
    ): ScheduledTaskAction.PresentInformational = ScheduledTaskAction.PresentInformational(component)

    fun createAnswerQuestionnaire(
        component: Component.Questionnaire = ComponentFixtures.createQuestionnaire(),
    ): ScheduledTaskAction.AnswerQuestionnaire = ScheduledTaskAction.AnswerQuestionnaire(component)

    fun createPromptTimedWalkingTest(
        component: Component.TimedWalkingTest = ComponentFixtures.createTimedWalkingTest(),
    ): ScheduledTaskAction.PromptTimedWalkingTest = ScheduledTaskAction.PromptTimedWalkingTest(component)

    fun createPerformCustomActiveTask(
        component: Component.CustomActiveTask = ComponentFixtures.createCustomActiveTask(),
    ): ScheduledTaskAction.PerformCustomActiveTask = ScheduledTaskAction.PerformCustomActiveTask(component)
}
