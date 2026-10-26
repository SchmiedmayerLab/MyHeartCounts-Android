//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.upcoming

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.StudyAppBarProviderImpl
import edu.stanford.myheartcounts.navigation.FakeNavigator
import edu.stanford.myheartcounts.task.EventTileMapperImpl
import edu.stanford.myheartcounts.task.FakeEventPerformer
import edu.stanford.myheartcounts.task.TaskListScreenState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.scheduler.Event
import org.grovealliance.scheduler.FakeScheduleCalculator
import org.grovealliance.scheduler.Schedule
import org.grovealliance.scheduler.fixtures.EventFixtures
import org.grovealliance.scheduler.fixtures.OccurrenceFixtures
import org.grovealliance.scheduler.fixtures.TaskFixtures
import org.grovealliance.study.StudyTaskCategories
import org.grovealliance.ui.scheduler.TaskListLayout
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class UpcomingTasksViewModelTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val now: Instant = Instant.parse("2026-08-01T10:00:00Z")
    private val tasksSource = FakeUpcomingTasksSource()
    private val eventPerformer = FakeEventPerformer()
    private val timeProvider = FakeTimeProvider().apply {
        setZone(zone)
        setNow(now)
    }
    private val tileMapper = EventTileMapperImpl(
        scheduleCalculator = FakeScheduleCalculator().apply {
            setZone(zone)
            setNow(now)
        },
        timeProvider = timeProvider,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher = UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `lists the upcoming events`() {
        // given
        tasksSource.setEvents(events = listOf(surveyTomorrow()))

        // when
        val viewModel = viewModel()

        // then
        assertThat(viewModel.content().sections.single().tiles).hasSize(1)
    }

    @Test
    fun `keeps the list current as the events change`() {
        // given
        tasksSource.setEvents(events = listOf(surveyTomorrow()))
        val viewModel = viewModel()

        // when
        tasksSource.setEvents(events = emptyList())

        // then
        assertThat(viewModel.content().sections).isEmpty()
    }

    @Test
    fun `performs an upcoming event through the event performer`() = runTest {
        // given
        val event = surveyTomorrow()
        tasksSource.setEvents(events = listOf(event))
        val viewModel = viewModel()

        // when
        viewModel.content().sections.single().tiles.single().action?.action?.invoke()

        // then
        assertThat(eventPerformer.requests).containsExactly(
            FakeEventPerformer.Request(
                event = event,
                completesEvent = true,
            )
        )
    }

    private fun viewModel() = UpcomingTasksViewModel(
        studyAppBarProvider = StudyAppBarProviderImpl(navigator = FakeNavigator()),
        tileMapper = tileMapper,
        tasksSource = tasksSource,
        mapper = UpcomingTasksMapperImpl(tileMapper = tileMapper),
        eventPerformer = eventPerformer,
    )

    private fun UpcomingTasksViewModel.content(): TaskListLayout =
        (screen.state.value as TaskListScreenState.Content).layout

    private fun surveyTomorrow(): Event {
        val start = now.plusSeconds(SECONDS_PER_DAY)
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

    private companion object {
        const val SECONDS_PER_DAY = 86_400L
    }
}
