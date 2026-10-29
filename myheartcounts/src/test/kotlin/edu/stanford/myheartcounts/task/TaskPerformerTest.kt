//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.task

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.navigation.FakeNavigator
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.study.StudyQuestionnaire
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Questionnaire
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskPerformerTest {

    private val navigator = FakeNavigator()
    private val performer = TaskPerformerImpl(navigator = navigator)
    private val task = PerformableTask.AnswerQuestionnaire(
        questionnaire = StudyQuestionnaire.prepare(questionnaire = Questionnaire()),
    )

    @Test
    fun `opens the task screen with the task active`() = runTest(UnconfinedTestDispatcher()) {
        // when
        val result = async { performer.perform(task = task) }

        // then
        assertThat(performer.activeTask.value?.task).isEqualTo(task)
        assertThat(navigator.pushedEvents).containsExactly(
            NavigationEvent.NavigateTo(route = MHCRoute.PerformTask),
        )
        result.cancel()
    }

    @Test
    fun `returns how the task finished`() = runTest(UnconfinedTestDispatcher()) {
        // given
        val result = async { performer.perform(task = task) }

        // when
        performer.activeTask.value?.finish(didSucceed = true)

        // then
        assertThat(result.await()).isTrue()
        assertThat(performer.activeTask.value).isNull()
    }

    @Test
    fun `counts only the first finish`() = runTest(UnconfinedTestDispatcher()) {
        // given
        val result = async { performer.perform(task = task) }
        val activeTask = performer.activeTask.value

        // when
        activeTask?.finish(didSucceed = false)
        activeTask?.finish(didSucceed = true)

        // then
        assertThat(result.await()).isFalse()
    }

    @Test
    fun `refuses a task while another is being performed`() = runTest(UnconfinedTestDispatcher()) {
        // given
        val first = async { performer.perform(task = task) }

        // when
        val second = performer.perform(task = task)

        // then
        assertThat(second).isFalse()
        assertThat(navigator.pushedEvents).hasSize(1)
        first.cancel()
    }

    @Test
    fun `clears the active task when the caller stops waiting`() = runTest(UnconfinedTestDispatcher()) {
        // given
        val result = async { performer.perform(task = task) }

        // when
        result.cancel()

        // then
        assertThat(performer.activeTask.value).isNull()
    }
}
