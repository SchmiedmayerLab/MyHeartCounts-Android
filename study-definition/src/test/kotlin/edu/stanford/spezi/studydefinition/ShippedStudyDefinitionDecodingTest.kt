//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition

import com.google.common.truth.Truth.assertThat
import edu.stanford.spezi.scheduler.NotificationThread
import org.junit.Test
import java.time.Duration

/**
 * Decodes the study definition the app ships, which is produced by the study authoring tooling rather
 * than hand-written, and therefore exercises the encoding in the form it actually arrives in.
 */
class ShippedStudyDefinitionDecodingTest {
    private companion object {
        const val RESOURCE = "/fixtures/mhcStudyDefinition.json"
        const val EXPECTED_REVISION = 40u
        const val EXPECTED_COMPONENTS = 20
        const val EXPECTED_SCHEDULES = 18
        const val ECG_IDENTIFIER = "edu.stanford.MyHeartCounts.activeTask.ecg"
    }

    private val definition: StudyDefinition = StudyDefinitionJson.decode(
        requireNotNull(javaClass.getResource(RESOURCE)) { "Missing test fixture: $RESOURCE" }.readText()
    )

    @Test
    fun `decodes the shipped study definition`() {
        // then
        assertThat(definition.studyRevision).isEqualTo(EXPECTED_REVISION)
        assertThat(definition.components).hasSize(EXPECTED_COMPONENTS)
        assertThat(definition.componentSchedules).hasSize(EXPECTED_SCHEDULES)
        assertThat(definition.metadata.title).isEqualTo("My Heart Counts")
    }

    @Test
    fun `resolves the electrocardiogram active task title`() {
        // when
        val activeTask = definition.components
            .filterIsInstance<Component.CustomActiveTask>()
            .single { it.activeTask.identifier == ECG_IDENTIFIER }
            .activeTask

        // then
        assertThat(activeTask.title).isEqualTo("ECG")
        assertThat(activeTask.subtitle).isEqualTo("Record an ECG using your Apple Watch")
    }

    @Test
    fun `decodes the six minute walk test duration`() {
        // when
        val walkTest = definition.components
            .filterIsInstance<Component.TimedWalkingTest>()
            .map { it.test }
            .single { it.kind == TimedWalkingTestConfiguration.Kind.WALKING }

        // then
        assertThat(walkTest.duration).isEqualTo(Duration.ofMinutes(6))
    }

    @Test
    fun `decodes a relative historical health data collection window`() {
        // when
        val historical = definition.healthDataCollectionComponents.single().historicalDataCollection

        // then
        assertThat(historical)
            .isInstanceOf(Component.HealthDataCollection.HistoricalDataCollection.Enabled::class.java)
        val startDate = (historical as Component.HealthDataCollection.HistoricalDataCollection.Enabled).startDate
        assertThat(startDate)
            .isEqualTo(Component.HealthDataCollection.HistoricalDataCollection.StartDate.Last(
                duration = DateComponents.EMPTY.copy(year = 10)
            ))
    }

    @Test
    fun `decodes per-task notification threads`() {
        // when
        val threads = definition.componentSchedules
            .map { it.notifications }
            .filterIsInstance<NotificationsConfig.Enabled>()
            .map { it.thread }

        // then
        assertThat(threads).isNotEmpty()
        assertThat(threads.toSet()).containsExactly(NotificationThread.PerTask)
    }

    @Test
    fun `decodes repeated schedule offsets`() {
        // when
        val offsets = definition.componentSchedules
            .map { it.scheduleDefinition }
            .filterIsInstance<ScheduleDefinition.Repeated>()
            .map { it.offset.day }

        // then
        assertThat(offsets).containsAtLeast(0, 1, 3, 4, 8)
    }
}
