//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.task

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.study.FakeStudyArticleSource
import edu.stanford.myheartcounts.study.FakeStudyQuestionnaireSource
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.grovealliance.core.coroutines.CoroutinesLauncher
import org.grovealliance.scheduler.Event
import org.grovealliance.scheduler.FakeScheduler
import org.grovealliance.scheduler.TaskContext
import org.grovealliance.scheduler.fixtures.EventFixtures
import org.grovealliance.scheduler.fixtures.TaskFixtures
import org.grovealliance.study.ScheduledTaskAction
import org.grovealliance.study.ScheduledTaskActionKey
import org.grovealliance.study.fixtures.ScheduledTaskActionFixtures
import org.grovealliance.ui.MutableGroveScaffoldState
import org.junit.Test

class EventPerformerTest {

    private val questionnaireSource = FakeStudyQuestionnaireSource()
    private val taskPerformer = FakeTaskPerformer()
    private val scheduler = FakeScheduler()
    private val performer = EventPerformerImpl(
        articleSource = FakeStudyArticleSource(),
        questionnaireSource = questionnaireSource,
        taskPerformer = taskPerformer,
        scheduler = scheduler,
    )

    @Test
    fun `completes a questionnaire event once it is submitted`() = runTest {
        // given
        taskPerformer.setPerformResult(didSucceed = true)

        // when
        performer.perform(
            event = eventWith(action = ScheduledTaskActionFixtures.createAnswerQuestionnaire()),
            completesEvent = true,
            scaffoldState = scaffoldState(),
        )

        // then
        assertThat(taskPerformer.performedTasks).hasSize(1)
        assertThat(scheduler.outcomes).hasSize(1)
    }

    @Test
    fun `leaves a questionnaire event open when it is not submitted`() = runTest {
        // given
        taskPerformer.setPerformResult(didSucceed = false)

        // when
        performer.perform(
            event = eventWith(action = ScheduledTaskActionFixtures.createAnswerQuestionnaire()),
            completesEvent = true,
            scaffoldState = scaffoldState(),
        )

        // then
        assertThat(scheduler.outcomes).isEmpty()
    }

    @Test
    fun `leaves the event open when performing it does not complete it`() = runTest {
        // given
        taskPerformer.setPerformResult(didSucceed = true)

        // when
        performer.perform(
            event = eventWith(action = ScheduledTaskActionFixtures.createAnswerQuestionnaire()),
            completesEvent = false,
            scaffoldState = scaffoldState(),
        )

        // then
        assertThat(scheduler.outcomes).isEmpty()
    }

    @Test
    fun `presents nothing when the questionnaire cannot be loaded`() = runTest {
        // given
        questionnaireSource.setResult(result = Result.failure(IllegalStateException("missing")))

        // when
        performer.perform(
            event = eventWith(action = ScheduledTaskActionFixtures.createAnswerQuestionnaire()),
            completesEvent = true,
            scaffoldState = scaffoldState(),
        )

        // then
        assertThat(taskPerformer.performedTasks).isEmpty()
        assertThat(scheduler.outcomes).isEmpty()
    }

    @Test
    fun `performs nothing for a task without a performer`() = runTest {
        // when
        performer.perform(
            event = eventWith(action = ScheduledTaskActionFixtures.createPromptTimedWalkingTest()),
            completesEvent = true,
            scaffoldState = scaffoldState(),
        )

        // then
        assertThat(taskPerformer.performedTasks).isEmpty()
        assertThat(scheduler.outcomes).isEmpty()
    }

    private fun TestScope.scaffoldState() = MutableGroveScaffoldState(
        coroutinesLauncher = CoroutinesLauncher(scope = backgroundScope),
    )

    private fun eventWith(action: ScheduledTaskAction): Event = EventFixtures.create(
        task = TaskFixtures.create(
            context = TaskContext().apply { this[ScheduledTaskActionKey] = action },
        ),
    )
}
