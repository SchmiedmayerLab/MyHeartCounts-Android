//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.studydefinition

import com.google.common.truth.Truth.assertThat
import org.grovealliance.foundation.JsonSerializer
import org.grovealliance.foundation.fixtures.UUIDFixtures
import org.grovealliance.scheduler.AllowedCompletionPolicy
import org.grovealliance.studydefinition.fixtures.StudyBundleFixtures
import org.junit.Test

class StudyDefinitionDecodingTest {

    private fun decodeExample(): StudyDefinition =
        StudyDefinitionJson.decode(StudyBundleFixtures.exampleDefinitionJson())

    @Test
    fun `decodes the example study definition`() {
        // when
        val definition = decodeExample()

        // then
        assertThat(definition.studyRevision).isEqualTo(1u)
        assertThat(definition.id).isEqualTo(UUIDFixtures.repeating('1'))
        assertThat(definition.metadata.title).isEqualTo("Example Study")
        assertThat(definition.components).hasSize(5)
        assertThat(definition.componentSchedules).hasSize(4)
    }

    @Test
    fun `decodes each component kind`() {
        // when
        val components = decodeExample().components

        // then
        assertThat(components.filterIsInstance<Component.Informational>()).hasSize(1)
        assertThat(components.filterIsInstance<Component.Questionnaire>()).hasSize(1)
        assertThat(components.filterIsInstance<Component.TimedWalkingTest>()).hasSize(1)
        assertThat(components.filterIsInstance<Component.CustomActiveTask>()).hasSize(1)
        assertThat(components.filterIsInstance<Component.HealthDataCollection>()).hasSize(1)
    }

    @Test
    fun `distinguishes user-interactive from internal components`() {
        // when
        val definition = decodeExample()

        // then
        assertThat(definition.healthDataCollectionComponents).hasSize(1)
        assertThat(definition.healthDataCollectionComponents.single().kind)
            .isEqualTo(Component.Kind.INTERNAL)
        assertThat(definition.components.filter { it.kind == Component.Kind.USER_INTERACTIVE }).hasSize(4)
    }

    @Test
    fun `decodes the custom active task identifier`() {
        // when
        val task = decodeExample().components.filterIsInstance<Component.CustomActiveTask>().single()

        // then
        assertThat(task.activeTask.identifier).isEqualTo("tapping")
        assertThat(task.activeTask.title).isEqualTo("Tapping Test")
    }

    @Test
    fun `decodes schedule definitions and completion policies`() {
        // when
        val schedules = decodeExample().componentSchedules.associateBy { it.componentId }
        val questionnaireComponentId = UUIDFixtures.repeating('3')
        val informationalComponentId = UUIDFixtures.repeating('2')

        // then
        val daily = schedules.getValue(questionnaireComponentId)
        assertThat(daily.completionPolicy).isEqualTo(AllowedCompletionPolicy.SAME_DAY)
        val repeated = daily.scheduleDefinition as ScheduleDefinition.Repeated
        assertThat(repeated.pattern).isInstanceOf(RepetitionPattern.Daily::class.java)

        val informational = schedules.getValue(informationalComponentId)
        assertThat(informational.completionPolicy).isEqualTo(AllowedCompletionPolicy.ANYTIME)
        val once = informational.scheduleDefinition as ScheduleDefinition.Once
        val event = once.schedule as OneTimeSchedule.Event
        assertThat(event.event).isEqualTo(StudyLifecycleEvent.Enrollment)
    }

    @Test
    fun `decodes the weekly schedule weekday`() {
        // when
        val walkComponentId = UUIDFixtures.repeating('4')
        val schedule = decodeExample().componentSchedules.single { it.componentId == walkComponentId }

        // then
        val weekly = (schedule.scheduleDefinition as ScheduleDefinition.Repeated).pattern
        assertThat(weekly).isInstanceOf(RepetitionPattern.Weekly::class.java)
        assertThat((weekly as RepetitionPattern.Weekly).weekday).isEqualTo(Weekday.MONDAY)
    }

    @Test
    fun `round-trips through encode and decode`() {
        // given
        val original = decodeExample()

        // when
        val encoded = JsonSerializer.encode(original, StudyDefinition.serializer())
        val decoded = StudyDefinitionJson.decode(encoded)

        // then
        assertThat(decoded).isEqualTo(original)
    }

    @Test(expected = IncompatibleSchemaException::class)
    fun `rejects an incompatible schema version`() {
        // given
        val json = StudyBundleFixtures.exampleDefinitionJson().replace("\"0.12.1\"", "\"0.11.0\"")

        // when / then
        StudyDefinitionJson.decode(json)
    }
}
