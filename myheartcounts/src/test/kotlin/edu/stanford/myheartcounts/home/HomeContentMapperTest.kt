//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.home

import com.google.common.truth.Truth.assertThat
import edu.stanford.spezi.scheduler.AllowedCompletionPolicy
import edu.stanford.spezi.scheduler.Event
import edu.stanford.spezi.scheduler.FakeScheduleCalculator
import edu.stanford.spezi.scheduler.Schedule
import edu.stanford.spezi.scheduler.TaskCategory
import edu.stanford.spezi.scheduler.fixtures.EventFixtures
import edu.stanford.spezi.scheduler.fixtures.OccurrenceFixtures
import edu.stanford.spezi.scheduler.fixtures.OutcomeFixtures
import edu.stanford.spezi.scheduler.fixtures.TaskFixtures
import edu.stanford.spezi.study.StudyTaskCategories
import edu.stanford.spezi.ui.ActionSource
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class HomeContentMapperTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val now: Instant = Instant.parse("2026-01-01T10:00:00Z")
    private val calculator = FakeScheduleCalculator().apply {
        setZone(zone)
        setNow(now)
    }
    private val mapper = HomeContentMapperImpl(scheduleCalculator = calculator)
    private val pushedActions = mutableListOf<HomeAction>()
    private val actionSink = ActionSource<HomeAction> { pushedActions.add(it) }.sink<HomeAction>()
    private val completedEvents = mutableListOf<Event>()

    @Test
    fun `renders a tile per event`() {
        // given
        val events = listOf(eventAt(now, StudyTaskCategories.questionnaire))

        // when
        val layout = mapper.map(input(events))

        // then
        assertThat(layout.tasks.sections.single().tiles).hasSize(1)
        assertThat(layout.tasks.emptyState).isNull()
    }

    @Test
    fun `shows the empty state when there is nothing to do`() {
        // when
        val layout = mapper.map(input(events = emptyList()))

        // then
        assertThat(layout.tasks.sections).isEmpty()
        assertThat(layout.tasks.emptyState).isNotNull()
    }

    @Test
    fun `disables today's task until its completion policy allows it`() {
        // given
        val events = listOf(
            eventAt(
                start = now.plusSeconds(SECONDS_PER_HOUR * 2),
                category = StudyTaskCategories.questionnaire,
                completionPolicy = AllowedCompletionPolicy.SAME_DAY_AFTER_START,
            )
        )

        // when
        val tile = mapper.map(input(events)).tasks.sections.single().tiles.single()

        // then
        assertThat(tile.action?.enabled).isFalse()
    }

    @Test
    fun `enables today's task once its completion policy allows it`() {
        // given
        val events = listOf(
            eventAt(
                start = now.plusSeconds(SECONDS_PER_HOUR * 2),
                category = StudyTaskCategories.questionnaire,
                completionPolicy = AllowedCompletionPolicy.ANYTIME,
            )
        )

        // when
        val tile = mapper.map(input(events)).tasks.sections.single().tiles.single()

        // then
        assertThat(tile.action?.enabled).isTrue()
    }

    @Test
    fun `omits the action of a completed non-active task`() {
        // given
        val event = eventAt(now, StudyTaskCategories.questionnaire).completed()

        // when
        val tile = mapper.map(input(listOf(event))).tasks.sections.single().tiles.single()

        // then
        assertThat(tile.isCompleted).isTrue()
        assertThat(tile.action).isNull()
    }

    @Test
    fun `keeps a completed active task repeatable`() {
        // given
        val event = eventAt(now, StudyTaskCategories.timedWalkingTest).completed()

        // when
        val tile = mapper.map(input(listOf(event))).tasks.sections.single().tiles.single()

        // then
        assertThat(tile.action?.enabled).isTrue()
    }

    @Test
    fun `offers the always-available tasks when nothing is scheduled`() {
        // when
        val layout = mapper.map(input(events = emptyList()))

        // then
        val section = layout.tasks.additionalSections.single()
        assertThat(section.tiles).hasSize(AlwaysAvailableTask.entries.size)
        assertThat(section.tiles.all { it.action != null }).isTrue()
    }

    @Test
    fun `hides the always-available tasks while today has tasks`() {
        // given
        val events = listOf(eventAt(now, StudyTaskCategories.questionnaire))

        // when
        val layout = mapper.map(input(events))

        // then
        assertThat(layout.tasks.additionalSections).isEmpty()
    }

    @Test
    fun `omits the missed row when nothing was missed`() {
        // when
        val layout = mapper.map(input(events = emptyList(), missedEventCount = 0))

        // then
        assertThat(layout.missedTasks).isNull()
    }

    @Test
    fun `shows the missed row when tasks were missed`() {
        // when
        val layout = mapper.map(input(events = emptyList(), missedEventCount = 3))

        // then
        assertThat(layout.missedTasks).isNotNull()
    }

    @Test
    fun `reports selections through the action sink`() {
        // given
        val layout = mapper.map(input(events = emptyList(), missedEventCount = 1))

        // when
        layout.missedTasks?.onClick?.invoke()
        layout.learnMore.onClicked()

        // then
        assertThat(pushedActions)
            .containsExactly(HomeAction.MissedTasksSelected, HomeAction.LearnMoreSelected)
            .inOrder()
    }

    @Test
    fun `completes a questionnaire from its tile`() = runTest {
        // given
        val event = eventAt(now, StudyTaskCategories.questionnaire)
        val tile = mapper.map(input(listOf(event))).tasks.sections.single().tiles.single()

        // when
        tile.action?.action?.invoke()

        // then
        assertThat(completedEvents).containsExactly(event)
        assertThat(pushedActions).isEmpty()
    }

    @Test
    fun `presents an article rather than completing it from its tile`() = runTest {
        // given
        val event = eventAt(now, StudyTaskCategories.informational)
        val tile = mapper.map(input(listOf(event))).tasks.sections.single().tiles.single()

        // when
        tile.action?.action?.invoke()

        // then
        assertThat(completedEvents).isEmpty()
        assertThat(pushedActions).containsExactly(
            HomeAction.ReadArticleSelected(
                event = event,
                completesEvent = true,
            )
        )
    }

    @Test
    fun `omits the nudge and prompted actions while nothing surfaces them`() {
        // when
        val layout = mapper.map(input(events = emptyList()))

        // then
        assertThat(layout.nudge).isNull()
        assertThat(layout.promptedActions).isNull()
    }

    private fun eventAt(
        start: Instant,
        category: TaskCategory,
        completionPolicy: AllowedCompletionPolicy = AllowedCompletionPolicy.ANYTIME,
    ): Event {
        val schedule = Schedule.once(at = start)
        return EventFixtures.create(
            task = TaskFixtures.create(
                title = "Task",
                category = category,
                schedule = schedule,
                completionPolicy = completionPolicy,
            ),
            occurrence = OccurrenceFixtures.create(start = start, schedule = schedule),
        )
    }

    private fun Event.completed() = copy(
        outcome = OutcomeFixtures.create(
            occurrenceStartDate = occurrence.start,
            completionDate = occurrence.start,
        )
    )

    private fun input(
        events: List<Event>,
        missedEventCount: Int = 0,
    ) = HomeContentInput(
        events = events,
        missedEventCount = missedEventCount,
        nudge = null,
        promptedActions = emptyList(),
        now = now,
        zone = zone,
        actionSink = actionSink,
        onCompleteEvent = { completedEvents.add(it) },
    )

    private companion object {
        const val SECONDS_PER_HOUR = 3_600L
    }
}
