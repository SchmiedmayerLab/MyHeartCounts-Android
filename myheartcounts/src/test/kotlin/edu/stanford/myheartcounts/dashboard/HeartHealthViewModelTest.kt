//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.FakeStudyAppBarProvider
import edu.stanford.myheartcounts.navigation.FakeNavigator
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.grovealliance.markdown.MarkdownDocument
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HeartHealthViewModelTest {
    private val scoreSource = FakeHeartHealthScoreSource()
    private val explainerSource = FakeHeartHealthExplainerSource()
    private val navigator = FakeNavigator()
    private val appBarProvider = FakeStudyAppBarProvider()
    private val mapper = FakeHeartHealthContentMapper()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher = UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `renders mapped participant scores`() {
        // given
        val scores = scores(overall = 0.7)
        scoreSource.setScores(scores)

        // when
        val viewModel = viewModel()

        // then
        assertThat(mapper.inputs.last().scores).isEqualTo(scores)
        assertThat(viewModel.screen.state.value).isInstanceOf(HeartHealthLayoutState.Content::class.java)
    }

    @Test
    fun `remaps the layout when scores change`() {
        // given
        scoreSource.setScores(scores(overall = 0.7))
        viewModel()
        val updated = scores(overall = 0.4)

        // when
        scoreSource.setScores(updated)

        // then
        assertThat(mapper.inputs.last().scores).isEqualTo(updated)
    }

    @Test
    fun `opens a factor's details`() {
        // given
        scoreSource.setScores(scores(overall = null))
        viewModel()

        // when
        mapper.inputs.last().actionSink.push(HeartHealthAction.ComponentSelected(component = HeartScoreComponent.Sleep))

        // then
        assertThat(navigator.pushedEvents).containsExactly(
            NavigationEvent.NavigateTo(route = MHCRoute.HeartScoreDetail(component = HeartScoreComponent.Sleep)),
        )
    }

    @Test
    fun `passes the study's explanation to the mapper`() {
        // given
        val document = MarkdownDocument.process(text = "#### What is this score?")
        explainerSource.setExplainer(explainer = document)
        scoreSource.setScores(scores(overall = null))

        // when
        viewModel()

        // then
        assertThat(mapper.inputs.last().explainer).isEqualTo(document)
    }

    @Test
    fun `maps scores without an explanation when none is provided`() {
        // given
        explainerSource.setExplainer(explainer = null)
        scoreSource.setScores(scores(overall = 0.7))

        // when
        viewModel()

        // then
        assertThat(mapper.inputs.last().explainer).isNull()
        assertThat(mapper.inputs.last().scores.overall).isEqualTo(0.7)
    }

    @Test
    fun `offers a retry when scores fail to load`() {
        // given
        scoreSource.fail(error = IllegalStateException("unavailable"))

        // when
        val viewModel = viewModel()

        // then
        assertThat(viewModel.screen.state.value).isInstanceOf(HeartHealthLayoutState.Error::class.java)
    }

    @Test
    fun `opens past test results`() {
        // given
        scoreSource.setScores(scores(overall = null))
        viewModel()

        // when
        mapper.inputs.last().actionSink.push(HeartHealthAction.PastDataSelected)

        // then
        assertThat(navigator.pushedEvents).containsExactly(NavigationEvent.NavigateTo(route = MHCRoute.PastTimedWalkTestResults))
    }

    private fun viewModel() = HeartHealthViewModel(
        studyAppBarProvider = appBarProvider,
        navigator = navigator,
        scoreSource = scoreSource,
        explainerSource = explainerSource,
        mapper = mapper,
    )

    private fun scores(overall: Double?) = HeartHealthFixtures.scores(
        overall = overall,
        scored = emptySet(),
        score = 0.0,
        measuredAt = HeartHealthScoreFixtures.NOW,
    )
}
