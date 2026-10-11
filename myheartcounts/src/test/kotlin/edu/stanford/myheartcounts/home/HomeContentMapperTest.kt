//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.home

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.task.AlwaysAvailableTask
import edu.stanford.myheartcounts.task.EventTileMapperImpl
import kotlinx.coroutines.test.runTest
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.scheduler.AllowedCompletionPolicy
import org.grovealliance.scheduler.Event
import org.grovealliance.scheduler.FakeScheduleCalculator
import org.grovealliance.scheduler.Schedule
import org.grovealliance.scheduler.TaskCategory
import org.grovealliance.scheduler.fixtures.EventFixtures
import org.grovealliance.scheduler.fixtures.OccurrenceFixtures
import org.grovealliance.scheduler.fixtures.OutcomeFixtures
import org.grovealliance.scheduler.fixtures.TaskFixtures
import org.grovealliance.study.StudyTaskCategories
import org.grovealliance.ui.ActionSource
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
    private val timeProvider = FakeTimeProvider().apply {
        setZone(zone)
        setNow(now)
    }
    private val mapper = HomeContentMapperImpl(
        tileMapper = EventTileMapperImpl(
            scheduleCalculator = calculator,
            timeProvider = timeProvider,
        ),
    )
    private val pushedActions = mutableListOf<HomeAction>()
    private val actionSink = ActionSource<HomeAction> { pushedActions.add(it) }.sink<HomeAction>()

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
                category = StudyTaskCategories.informational,
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
                category = StudyTaskCategories.informational,
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
    fun `disables the action of every task that cannot be performed`() {
        // given
        val events = UNPERFORMABLE_CATEGORIES.map { eventAt(now, it) }

        // when
        val tiles = mapper.map(input(events)).tasks.sections.single().tiles

        // then
        assertThat(tiles.map { it.action?.enabled }).containsExactly(false, false, false)
    }

    @Test
    fun `omits the action of a completed task that cannot be performed`() {
        // given
        val event = eventAt(now, StudyTaskCategories.timedWalkingTest).completed()

        // when
        val tile = mapper.map(input(listOf(event))).tasks.sections.single().tiles.single()

        // then
        assertThat(tile.action).isNull()
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
    fun `raises nothing from the tile of a task that cannot be performed`() = runTest {
        // given
        val events = UNPERFORMABLE_CATEGORIES.map { eventAt(now, it) }
        val tiles = mapper.map(input(events)).tasks.sections.single().tiles

        // when
        tiles.forEach { it.action?.action?.invoke() }

        // then
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
        assertThat(pushedActions).containsExactly(
            HomeAction.PerformTaskSelected(
                event = event,
                completesEvent = true,
            )
        )
    }

    @Test
    fun `presents a questionnaire rather than completing it from its tile`() = runTest {
        // given
        val event = eventAt(now, StudyTaskCategories.questionnaire)
        val tile = mapper.map(input(listOf(event))).tasks.sections.single().tiles.single()

        // when
        tile.action?.action?.invoke()

        // then
        assertThat(tile.action?.enabled).isTrue()
        assertThat(pushedActions).containsExactly(
            HomeAction.PerformTaskSelected(
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
        actionSink = actionSink,
    )

    private companion object {
        const val SECONDS_PER_HOUR = 3_600L
        val UNPERFORMABLE_CATEGORIES = listOf(
            StudyTaskCategories.timedWalkingTest,
            StudyTaskCategories.timedRunningTest,
            StudyTaskCategories.customActiveTask(identifier = "edu.stanford.MyHeartCounts.activeTask.ecg"),
        )
    }
}
