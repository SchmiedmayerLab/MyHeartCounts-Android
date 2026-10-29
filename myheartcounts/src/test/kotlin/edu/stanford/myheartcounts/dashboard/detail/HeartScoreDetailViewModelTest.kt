//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.dashboard.FakeHeartHealthScoreSource
import edu.stanford.myheartcounts.dashboard.HeartHealthScores
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import edu.stanford.myheartcounts.navigation.FakeNavigator
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HeartScoreDetailViewModelTest {
    private val scores = FakeHeartHealthScoreSource()
    private val charts = FakeHeartScoreChartSource()
    private val explainer = FakeHeartScoreExplainerSource()
    private val debug = FakeHeartScoreDebugSource()
    private val mapper = FakeHeartScoreDetailContentMapper()
    private val chartMapper = FakeHeartScoreChartMapper()
    private val navigator = FakeNavigator()
    private val saved = SavedStateHandle(mapOf(HeartScoreDetailViewModel.KEY_COMPONENT to HeartScoreComponent.Sleep.name))

    @Before fun setup() { Dispatchers.setMain(UnconfinedTestDispatcher()) }

    @After fun teardown() { Dispatchers.resetMain() }

    @Test
    fun `renders current measurement with a fourteen day chart`() {
        // given
        supplyScore()

        // when
        val vm = viewModel()

        // then
        assertThat(vm.screen.state.value).isInstanceOf(HeartScoreDetailLayoutState.Content::class.java)
        assertThat(mapper.inputs.last().result.measurement).isNotNull()
        assertThat(chartMapper.inputs.single().selection.value).isEqualTo(HeartScoreChartRange.Fortnight)
        assertThat(charts.requests).containsExactly(HeartScoreChartRange.Fortnight)
    }

    @Test
    fun `selection restarts history and persists the choice`() {
        // given
        supplyScore()
        viewModel()

        // when
        mapper.inputs.last().actionSink.push(HeartScoreDetailAction.RangeSelected(range = HeartScoreChartRange.Year))

        // then
        assertThat(charts.requests.last()).isEqualTo(HeartScoreChartRange.Year)
        assertThat(saved.get<String>(HeartScoreDetailViewModel.KEY_RANGE)).isEqualTo(HeartScoreChartRange.Year.name)
    }

    @Test
    fun `selection keeps the chart and the layout`() {
        // given
        supplyScore()
        viewModel()
        val layouts = mapper.inputs.size

        // when
        mapper.inputs.last().actionSink.push(HeartScoreDetailAction.RangeSelected(range = HeartScoreChartRange.Year))

        // then
        assertThat(mapper.inputs).hasSize(layouts)
        assertThat(chartMapper.inputs.single().selection.value).isEqualTo(HeartScoreChartRange.Year)
        assertThat(chartMapper.inputs.single().data.value).isNotNull()
    }

    @Test
    fun `restores a saved range`() {
        // given
        saved[HeartScoreDetailViewModel.KEY_RANGE] = HeartScoreChartRange.Quarter.name
        supplyScore()

        // when
        viewModel()

        // then
        assertThat(charts.requests).containsExactly(HeartScoreChartRange.Quarter)
    }

    @Test
    fun `old range cannot overwrite a newly selected range`() {
        // given
        supplyScore()
        viewModel()
        mapper.inputs.last().actionSink.push(HeartScoreDetailAction.RangeSelected(range = HeartScoreChartRange.Year))

        // when
        charts.fail(selection = HeartScoreChartRange.Fortnight)

        // then
        assertThat(chartMapper.inputs.single().data.value).isNotNull()
    }

    @Test
    fun `chart failure empties the chart and retains the header and explanation`() {
        // given
        supplyScore()
        val vm = viewModel()

        // when
        charts.fail(selection = HeartScoreChartRange.Fortnight)

        // then
        assertThat(chartMapper.inputs.single().data.value).isNull()
        assertThat(vm.screen.state.value).isInstanceOf(HeartScoreDetailLayoutState.Content::class.java)
    }

    @Test
    fun `diet and nicotine do not subscribe to chart history`() {
        for (component in listOf(HeartScoreComponent.Diet, HeartScoreComponent.NicotineExposure)) {
            // given
            saved[HeartScoreDetailViewModel.KEY_COMPONENT] = component.name
            supplyScore(component = component)

            // when
            viewModel()

            // then
            assertThat(mapper.inputs.last().chart).isNull()
        }
        assertThat(charts.requests).isEmpty()
        assertThat(chartMapper.inputs).isEmpty()
    }

    @Test
    fun `opens external reading links and closes the same component route`() {
        // given
        supplyScore()
        viewModel()

        // when
        mapper.inputs.last().actionSink.push(HeartScoreDetailAction.LinkSelected(url = "https://example.com"))
        mapper.inputs.last().actionSink.push(HeartScoreDetailAction.Close)

        // then
        assertThat(navigator.pushedEvents).containsExactly(
            NavigationEvent.OpenUrl(url = "https://example.com"),
            NavigationEvent.Close(route = MHCRoute.HeartScoreDetail(component = HeartScoreComponent.Sleep)),
        ).inOrder()
    }

    @Test
    fun `retries a failed initial load`() {
        // given
        supplyScore()
        explainer.setFailure(failure = IllegalStateException("unavailable"))
        val vm = viewModel()
        assertThat(vm.screen.state.value).isInstanceOf(HeartScoreDetailLayoutState.Error::class.java)

        // when
        explainer.setFailure(failure = null)
        val state = vm.screen.state.value as HeartScoreDetailLayoutState.Error
        kotlinx.coroutines.test.runTest { state.layout.primaryButton!!.action() }

        // then
        assertThat(vm.screen.state.value).isInstanceOf(HeartScoreDetailLayoutState.Content::class.java)
    }

    private fun supplyScore(component: HeartScoreComponent = HeartScoreComponent.Sleep) {
        scores.setScores(HeartHealthScores(
            overall = null,
            components = listOf(HeartScoreDetailFixtures.result(component = component, value = 8.5)),
        ))
    }

    private fun viewModel() = HeartScoreDetailViewModel(
        savedStateHandle = saved,
        navigator = navigator,
        scoreSource = scores,
        chartSource = charts,
        explainerSource = explainer,
        mapper = mapper,
        chartMapper = chartMapper,
        debugSource = debug,
        questionnaires = edu.stanford.myheartcounts.study.FakeStudyQuestionnaireSource(),
        taskPerformer = edu.stanford.myheartcounts.task.FakeTaskPerformer(),
        ioDispatcher = UnconfinedTestDispatcher(),
    )
}
