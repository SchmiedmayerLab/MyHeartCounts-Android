//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.missed

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.task.AlwaysAvailableTask
import edu.stanford.myheartcounts.task.EventTileMapperImpl
import kotlinx.coroutines.test.runTest
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.scheduler.Event
import org.grovealliance.scheduler.FakeScheduleCalculator
import org.grovealliance.scheduler.Schedule
import org.grovealliance.scheduler.fixtures.EventFixtures
import org.grovealliance.scheduler.fixtures.OccurrenceFixtures
import org.grovealliance.scheduler.fixtures.TaskFixtures
import org.grovealliance.study.StudyTaskCategories
import org.grovealliance.ui.ActionSource
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class MissedTasksMapperTest {

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
    private val mapper = MissedTasksMapperImpl(
        tileMapper = EventTileMapperImpl(
            scheduleCalculator = calculator,
            timeProvider = timeProvider,
        ),
    )
    private val pushedActions = mutableListOf<MissedTasksAction>()
    private val actionSink = ActionSource<MissedTasksAction> { pushedActions.add(it) }.sink<MissedTasksAction>()

    @Test
    fun `groups the missed events by the day they occurred on`() {
        // given
        val events = listOf(
            eventAt(start = now.minusSeconds(SECONDS_PER_DAY * 2)),
            eventAt(start = now.minusSeconds(SECONDS_PER_DAY)),
            eventAt(start = now.minusSeconds(SECONDS_PER_DAY).plusSeconds(SECONDS_PER_HOUR)),
        )

        // when
        val sections = mapper.map(input(events)).sections

        // then
        assertThat(sections.map { it.tiles.size }).containsExactly(1, 2).inOrder()
        assertThat(sections.all { it.title != null }).isTrue()
    }

    @Test
    fun `offers the always-available tasks when nothing was missed`() {
        // when
        val tasks = mapper.map(input(events = emptyList()))

        // then
        assertThat(tasks.sections).isEmpty()
        assertThat(tasks.emptyState).isNotNull()
        assertThat(tasks.additionalSections.single().tiles).hasSize(AlwaysAvailableTask.entries.size)
    }

    @Test
    fun `hides the always-available tasks while tasks were missed`() {
        // when
        val tasks = mapper.map(input(events = listOf(eventAt(start = now.minusSeconds(SECONDS_PER_DAY)))))

        // then
        assertThat(tasks.emptyState).isNull()
        assertThat(tasks.additionalSections).isEmpty()
    }

    @Test
    fun `asks to perform a missed questionnaire, completing it`() = runTest {
        // given
        val event = eventAt(start = now.minusSeconds(SECONDS_PER_DAY))
        val tile = mapper.map(input(listOf(event))).sections.single().tiles.single()

        // when
        tile.action?.action?.invoke()

        // then
        assertThat(pushedActions).containsExactly(
            MissedTasksAction.PerformTaskSelected(
                event = event,
                completesEvent = true,
            )
        )
    }

    private fun eventAt(start: Instant): Event {
        val schedule = Schedule.once(at = start)
        return EventFixtures.create(
            task = TaskFixtures.create(
                title = "Survey",
                category = StudyTaskCategories.questionnaire,
                schedule = schedule,
            ),
            occurrence = OccurrenceFixtures.create(start = start, schedule = schedule),
        )
    }

    private fun input(events: List<Event>) = MissedTasksInput(
        events = events,
        actionSink = actionSink,
    )

    private companion object {
        const val SECONDS_PER_DAY = 86_400L
        const val SECONDS_PER_HOUR = 3_600L
    }
}
