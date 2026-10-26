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
import edu.stanford.myheartcounts.standard.FakeQuestionnaireResponseUploader
import edu.stanford.myheartcounts.study.StudyQuestionnaire
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.grovealliance.questionnaire.QuestionnaireResult
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PerformTaskViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val taskPerformer = FakeTaskPerformer()
    private val navigator = FakeNavigator()
    private val uploader = FakeQuestionnaireResponseUploader()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher = dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shows an error when no task is active`() {
        // when
        val viewModel = viewModel()

        // then
        assertThat(viewModel.screen).isInstanceOf(PerformTaskScreenState.Error::class.java)
        assertThat(navigator.pushedEvents).isEmpty()
    }

    @Test
    fun `closes from the error`() = runTest {
        // given
        val viewModel = viewModel()
        val error = viewModel.screen as PerformTaskScreenState.Error

        // when
        error.layout.primaryButton.action()

        // then
        assertThat(navigator.pushedEvents).containsExactly(NavigationEvent.Close(route = MHCRoute.PerformTask))
    }

    @Test
    fun `renders the active questionnaire`() {
        // given
        taskPerformer.setActiveTask(task = questionnaireTask())

        // when
        val viewModel = viewModel()

        // then
        assertThat(viewModel.screen).isInstanceOf(PerformTaskScreenState.Content::class.java)
        assertThat(navigator.pushedEvents).isEmpty()
    }

    @Test
    fun `uploads a submitted response, completes the task and closes`() {
        // given
        taskPerformer.setActiveTask(task = questionnaireTask())
        val viewModel = viewModel()

        // when
        viewModel.questionnaireContent().onResult(QuestionnaireResult.Completed(response = QuestionnaireResponse()))

        // then
        assertThat(uploader.uploads).hasSize(1)
        assertThat(taskPerformer.outcomes).containsExactly(true)
        assertThat(navigator.pushedEvents).containsExactly(NavigationEvent.Close(route = MHCRoute.PerformTask))
    }

    @Test
    fun `leaves the task uncompleted when the questionnaire is cancelled`() {
        // given
        taskPerformer.setActiveTask(task = questionnaireTask())
        val viewModel = viewModel()

        // when
        viewModel.questionnaireContent().onResult(QuestionnaireResult.Cancelled)

        // then
        assertThat(uploader.uploads).isEmpty()
        assertThat(taskPerformer.outcomes).containsExactly(false)
        assertThat(navigator.pushedEvents).containsExactly(NavigationEvent.Close(route = MHCRoute.PerformTask))
    }

    @Test
    fun `closes when the task ends elsewhere`() {
        // given
        taskPerformer.setActiveTask(task = questionnaireTask())
        viewModel()

        // when
        taskPerformer.clearActiveTask()

        // then
        assertThat(taskPerformer.outcomes).isEmpty()
        assertThat(navigator.pushedEvents).containsExactly(NavigationEvent.Close(route = MHCRoute.PerformTask))
    }

    @Test
    fun `leaves the task uncompleted when the questionnaire fails`() {
        // given
        taskPerformer.setActiveTask(task = questionnaireTask())
        val viewModel = viewModel()

        // when
        viewModel.questionnaireContent().onResult(QuestionnaireResult.Failed)

        // then
        assertThat(uploader.uploads).isEmpty()
        assertThat(taskPerformer.outcomes).containsExactly(false)
        assertThat(navigator.pushedEvents).containsExactly(NavigationEvent.Close(route = MHCRoute.PerformTask))
    }

    private fun viewModel() = PerformTaskViewModel(
        taskPerformer = taskPerformer,
        navigator = navigator,
        responseUploader = uploader,
        uploadScope = TestScope(dispatcher),
    )

    private fun questionnaireTask() = PerformableTask.AnswerQuestionnaire(
        questionnaire = StudyQuestionnaire.prepare(questionnaire = Questionnaire().setTitle("Survey")),
    )

    private fun PerformTaskViewModel.questionnaireContent() = (screen as PerformTaskScreenState.Content).layout
}
