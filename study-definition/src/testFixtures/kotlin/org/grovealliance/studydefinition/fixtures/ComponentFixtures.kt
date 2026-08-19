//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.studydefinition.fixtures

import org.grovealliance.foundation.fixtures.UUIDFixtures
import org.grovealliance.studydefinition.Component
import org.grovealliance.studydefinition.FileReference
import org.grovealliance.studydefinition.TimedWalkingTestConfiguration
import java.util.UUID

/**
 * Fixtures for [Component]. [create] returns a [Component.Informational].
 */
object ComponentFixtures {
    fun create(): Component = createInformational()

    fun createInformational(
        id: UUID = UUIDFixtures.zero,
        fileRef: FileReference = FileReferenceFixtures.create(),
    ): Component.Informational = Component.Informational(
        id = id,
        fileRef = fileRef,
    )

    fun createQuestionnaire(
        id: UUID = UUIDFixtures.zero,
        fileRef: FileReference = FileReferenceFixtures.create(),
    ): Component.Questionnaire = Component.Questionnaire(
        id = id,
        fileRef = fileRef,
    )

    fun createHealthDataCollection(
        id: UUID = UUIDFixtures.zero,
        sampleTypes: List<String> = emptyList(),
        optionalSampleTypes: List<String> = emptyList(),
        historicalDataCollection: Component.HealthDataCollection.HistoricalDataCollection =
            HistoricalDataCollectionFixtures.create(),
    ): Component.HealthDataCollection = Component.HealthDataCollection(
        id = id,
        sampleTypes = sampleTypes,
        optionalSampleTypes = optionalSampleTypes,
        historicalDataCollection = historicalDataCollection,
    )

    fun createTimedWalkingTest(
        id: UUID = UUIDFixtures.zero,
        test: TimedWalkingTestConfiguration = TimedWalkingTestConfigurationFixtures.create(),
    ): Component.TimedWalkingTest = Component.TimedWalkingTest(
        id = id,
        test = test,
    )

    fun createCustomActiveTask(
        id: UUID = UUIDFixtures.zero,
        activeTask: Component.CustomActiveTask.ActiveTask = ActiveTaskFixtures.create(),
    ): Component.CustomActiveTask = Component.CustomActiveTask(
        id = id,
        activeTask = activeTask,
    )
}
