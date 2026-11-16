//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.promptedactions

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.navigation.FakeNavigator
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class PromptedActionsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val source = FakePromptedActionsSource()
    private val rejections = FakePromptedActionRejections()
    private val navigator = FakeNavigator()
    private val mapper = FakePromptedActionsMapper()
    private val demographics = PromptedActionFixtures.create(type = PromptedActionType.CompleteDemographics)
    private val verifyEmail = PromptedActionFixtures.create(type = PromptedActionType.VerifyAccountEmail)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        source.setActions(actions = listOf(demographics, verifyEmail))
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `opens the demographics to complete them`() = runTest(dispatcher) {
        // given
        val viewModel = viewModel()
        runCurrent()

        // when
        requireNotNull(viewModel.rows().first().action).action()

        // then
        assertThat(navigator.pushedEvents).containsExactly(NavigationEvent.NavigateTo(route = MHCRoute.Demographics))
    }

    @Test
    fun `opens the email verification to verify the email`() = runTest(dispatcher) {
        // given
        val viewModel = viewModel()
        runCurrent()

        // when
        requireNotNull(viewModel.rows().last().action).action()

        // then
        assertThat(navigator.pushedEvents).containsExactly(NavigationEvent.NavigateTo(route = MHCRoute.VerifyEmail))
    }

    @Test
    fun `asks to confirm before it stops suggesting an action`() = runTest(dispatcher) {
        // given
        val viewModel = viewModel()
        runCurrent()

        // when
        requireNotNull(viewModel.rows().first().onStopSuggesting).invoke()
        runCurrent()

        // then
        assertThat(viewModel.dialog()).isNotNull()
        assertThat(rejections.rejected.value).isEmpty()
    }

    @Test
    fun `stops suggesting an action once confirmed`() = runTest(dispatcher) {
        // given
        val viewModel = viewModel()
        runCurrent()
        requireNotNull(viewModel.rows().first().onStopSuggesting).invoke()
        runCurrent()

        // when
        mapper.stopSuggestingDialogs.single().confirmButton.action()
        runCurrent()

        // then
        assertThat(rejections.rejected.value).containsExactly(PromptedActionType.CompleteDemographics)
    }

    @Test
    fun `keeps suggesting an action when the confirmation is dismissed`() = runTest(dispatcher) {
        // given
        val viewModel = viewModel()
        runCurrent()
        requireNotNull(viewModel.rows().first().onStopSuggesting).invoke()
        runCurrent()

        // when
        requireNotNull(viewModel.dialog()).onDismiss()
        runCurrent()

        // then
        assertThat(rejections.rejected.value).isEmpty()
        assertThat(viewModel.dialog()).isNull()
    }

    @Test
    fun `closes itself shortly after the last pending action is gone`() = runTest(dispatcher) {
        // given
        viewModel()
        runCurrent()

        // when
        source.setActions(actions = listOf(demographics.copy(isRejected = true), verifyEmail.copy(state = PromptedActionState.Completed)))
        runCurrent()
        advanceTimeBy(delayTime = 500.milliseconds)
        runCurrent()

        // then
        assertThat(navigator.pushedEvents).containsExactly(
            NavigationEvent.Close(route = MHCRoute.PromptedActions(mode = PromptedActionsMode.Pending)),
        )
    }

    @Test
    fun `stays open when opened without pending actions`() = runTest(dispatcher) {
        // given
        source.setActions(actions = listOf(demographics.copy(state = PromptedActionState.Completed)))

        // when
        viewModel(mode = PromptedActionsMode.All)
        runCurrent()
        advanceTimeBy(delayTime = 500.milliseconds)
        runCurrent()

        // then
        assertThat(navigator.pushedEvents).isEmpty()
    }

    private fun viewModel(mode: PromptedActionsMode = PromptedActionsMode.Pending) = PromptedActionsViewModel(
        savedStateHandle = SavedStateHandle(mapOf(PromptedActionsViewModel.KEY_MODE to mode.name)),
        source = source,
        rejections = rejections,
        navigator = navigator,
        mapper = mapper,
    )

    private fun PromptedActionsViewModel.rows() = screen.sections.value.sections.first().actionRows()

    private fun PromptedActionsViewModel.dialog() = screen.scaffoldState.widgets.value.dialog
}
