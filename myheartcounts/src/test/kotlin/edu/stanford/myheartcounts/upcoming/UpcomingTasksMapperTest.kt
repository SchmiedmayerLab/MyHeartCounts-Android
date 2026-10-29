//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.upcoming

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.task.AlwaysAvailableTask
import edu.stanford.myheartcounts.task.EventTileMapperImpl
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.scheduler.Event
import org.grovealliance.scheduler.FakeScheduleCalculator
import org.grovealliance.scheduler.Schedule
import org.grovealliance.scheduler.TaskCategory
import org.grovealliance.scheduler.TaskContext
import org.grovealliance.scheduler.fixtures.EventFixtures
import org.grovealliance.scheduler.fixtures.OccurrenceFixtures
import org.grovealliance.scheduler.fixtures.TaskFixtures
import org.grovealliance.study.ScheduledTaskActionKey
import org.grovealliance.study.StudyTaskCategories
import org.grovealliance.study.fixtures.ScheduledTaskActionFixtures
import org.grovealliance.studydefinition.TimedWalkingTestConfiguration
import org.grovealliance.studydefinition.fixtures.ComponentFixtures
import org.grovealliance.studydefinition.fixtures.TimedWalkingTestConfigurationFixtures
import org.grovealliance.ui.ActionSource
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class UpcomingTasksMapperTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val now: Instant = Instant.parse("2026-08-01T10:00:00Z")
    private val calculator = FakeScheduleCalculator().apply {
        setZone(zone)
        setNow(now)
    }
    private val timeProvider = FakeTimeProvider().apply {
        setZone(zone)
        setNow(now)
    }
    private val mapper = UpcomingTasksMapperImpl(
        tileMapper = EventTileMapperImpl(
            scheduleCalculator = calculator,
            timeProvider = timeProvider,
        ),
    )
    private val pushedActions = mutableListOf<UpcomingTasksAction>()
    private val actionSink = ActionSource<UpcomingTasksAction> { pushedActions.add(it) }.sink<UpcomingTasksAction>()

    @Test
    fun `groups the upcoming events by the day they occur on`() {
        // given
        val events = listOf(
            eventAt(start = now.plusSeconds(SECONDS_PER_HOUR)),
            eventAt(start = now.plusSeconds(SECONDS_PER_DAY)),
            eventAt(start = now.plusSeconds(SECONDS_PER_DAY + SECONDS_PER_HOUR)),
        )

        // when
        val sections = mapper.map(input(events)).sections

        // then
        assertThat(sections.map { it.tiles.size }).containsExactly(1, 2).inOrder()
    }

    @Test
    fun `says so and offers every always-available task when nothing is upcoming`() {
        // when
        val layout = mapper.map(input(events = emptyList()))

        // then
        assertThat(layout.emptyState).isNotNull()
        assertThat(layout.additionalSections.single().tiles).hasSize(AlwaysAvailableTask.entries.size)
    }

    @Test
    fun `offers the always-available tasks alongside upcoming events`() {
        // when
        val layout = mapper.map(input(events = listOf(eventAt(start = now.plusSeconds(SECONDS_PER_HOUR)))))

        // then
        assertThat(layout.emptyState).isNull()
        assertThat(layout.additionalSections.single().tiles).hasSize(AlwaysAvailableTask.entries.size)
    }

    @Test
    fun `leaves out an always-available electrocardiogram already scheduled`() {
        // given
        val ecg = eventAt(
            start = now.plusSeconds(SECONDS_PER_HOUR),
            category = StudyTaskCategories.customActiveTask(identifier = AlwaysAvailableTask.ECG_IDENTIFIER),
        )

        // when
        val tiles = mapper.map(input(events = listOf(ecg))).additionalSections.single().tiles

        // then
        assertThat(tiles).hasSize(AlwaysAvailableTask.entries.size - 1)
    }

    @Test
    fun `leaves out an always-available timed test already scheduled`() {
        // given
        val walk = eventAt(
            start = now.plusSeconds(SECONDS_PER_HOUR),
            category = StudyTaskCategories.timedWalkingTest,
            context = TaskContext().apply {
                this[ScheduledTaskActionKey] = ScheduledTaskActionFixtures.createPromptTimedWalkingTest(
                    component = ComponentFixtures.createTimedWalkingTest(
                        test = TimedWalkingTestConfigurationFixtures.create(
                            duration = Duration.ofMinutes(SIX_MINUTES),
                            kind = TimedWalkingTestConfiguration.Kind.WALKING,
                        ),
                    ),
                )
            },
        )

        // when
        val tiles = mapper.map(input(events = listOf(walk))).additionalSections.single().tiles

        // then
        assertThat(tiles).hasSize(AlwaysAvailableTask.entries.size - 1)
    }

    private fun eventAt(
        start: Instant,
        category: TaskCategory = StudyTaskCategories.questionnaire,
        context: TaskContext = TaskContext(),
    ): Event {
        val schedule = Schedule.once(at = start)
        return EventFixtures.create(
            task = TaskFixtures.create(
                title = "Task",
                category = category,
                schedule = schedule,
                context = context,
            ),
            occurrence = OccurrenceFixtures.create(start = start, schedule = schedule),
        )
    }

    private fun input(events: List<Event>) = UpcomingTasksInput(
        events = events,
        actionSink = actionSink,
    )

    private companion object {
        const val SECONDS_PER_DAY = 86_400L
        const val SECONDS_PER_HOUR = 3_600L
        const val SIX_MINUTES = 6L
    }
}
