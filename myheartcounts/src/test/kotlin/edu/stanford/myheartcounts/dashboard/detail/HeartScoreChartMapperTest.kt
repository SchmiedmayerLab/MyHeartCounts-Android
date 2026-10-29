//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import edu.stanford.myheartcounts.dashboard.ui.DashboardColorPalette
import edu.stanford.myheartcounts.dashboard.ui.chart.CartesianChart
import edu.stanford.myheartcounts.dashboard.ui.chart.ChartCard
import edu.stanford.myheartcounts.dashboard.ui.chart.TimeCoordinates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.StringResource
import org.junit.Test
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class HeartScoreChartMapperTest {
    private val time = FakeTimeProvider()
    private val ranges = FakeHeartScoreDetailRanges()
    private val mapper = HeartScoreChartMapperImpl(
        timeProvider = time,
        ranges = ranges,
    )
    private val actions = mutableListOf<HeartScoreDetailAction>()
    private val sink = ActionSource<HeartScoreDetailAction> { actions += it }.sink<HeartScoreDetailAction>()

    @Test
    fun `steps and sleep use zero-based bars and other quantities use lines`() = runTest(UnconfinedTestDispatcher()) {
        for (component in listOf(HeartScoreComponent.StepCount, HeartScoreComponent.Sleep, HeartScoreComponent.BloodLipids)) {
            // given
            val data = HeartScoreDetailFixtures.chart(points = listOf(HeartScoreChartPoint(
                timeRange = HeartScoreDetailFixtures.RANGE,
                value = 100.0,
            )))

            // when
            val chart = chart(content = mapper.map(input = input(scope = backgroundScope, component = component, data = data)))

            // then
            val expected = if (component == HeartScoreComponent.BloodLipids) CartesianChart.Style.Line else CartesianChart.Style.Bar
            assertThat(chart.series.single().style).isEqualTo(expected)
            if (expected == CartesianChart.Style.Bar) assertThat(chart.yAxis.range.start).isAtMost(0.0)
        }
    }

    @Test
    fun `range menu reports the selected calendar range`() = runTest(UnconfinedTestDispatcher()) {
        // when
        val content = mapper.map(
            input = input(
                scope = backgroundScope,
                component = HeartScoreComponent.Sleep,
                data = HeartScoreDetailFixtures.chart(points = emptyList()),
            ),
        )
        content.picker.value!!.onSelected(5)

        // then
        assertThat(actions).containsExactly(HeartScoreDetailAction.RangeSelected(range = HeartScoreChartRange.Year))
    }

    @Test
    fun `points are sorted and non-finite values do not reach the renderer`() = runTest(UnconfinedTestDispatcher()) {
        // given
        val point = HeartScoreChartPoint(
            timeRange = HeartScoreDetailFixtures.RANGE,
            value = Double.NaN,
        )

        // when
        val chart = chart(
            content = mapper.map(
                input = input(
                    scope = backgroundScope,
                    component = HeartScoreComponent.Sleep,
                    data = HeartScoreDetailFixtures.chart(points = listOf(point)),
                ),
            ),
        )

        // then
        assertThat(chart.series.single().points).isEmpty()
    }

    @Test
    fun `pressure distinguishes red systolic and blue diastolic readings`() = runTest(UnconfinedTestDispatcher()) {
        // given
        val data = HeartScoreChartData(
            timeRange = HeartScoreDetailFixtures.RANGE,
            series = listOf(HeartScoreChartSeries.Kind.Systolic, HeartScoreChartSeries.Kind.Diastolic).map {
                HeartScoreChartSeries(kind = it, points = emptyList())
            },
        )

        // when
        val chart = chart(
            content = mapper.map(
                input = input(
                    scope = backgroundScope,
                    component = HeartScoreComponent.BloodPressure,
                    data = data,
                ),
            ),
        )

        // then
        assertThat(chart.series.map { it.color }).containsExactly(DashboardColorPalette.red, DashboardColorPalette.blue).inOrder()
    }

    @Test
    fun `the card follows the chosen range and its history`() = runTest(UnconfinedTestDispatcher()) {
        // given
        val data = MutableStateFlow<HeartScoreChartData?>(HeartScoreDetailFixtures.chart(points = emptyList()))
        val selection = MutableStateFlow(HeartScoreChartRange.Fortnight)
        val card = mapper.map(
            input = HeartScoreChartInput(
                component = HeartScoreComponent.Sleep,
                data = data,
                selection = selection,
                scope = backgroundScope,
                actionSink = sink,
            ),
        )

        // when
        selection.value = HeartScoreChartRange.Year
        data.value = HeartScoreDetailFixtures.chart(
            points = listOf(
                HeartScoreChartPoint(
                    timeRange = HeartScoreDetailFixtures.RANGE,
                    value = 5.0,
                ),
            ),
        )

        // then
        assertThat(card.picker.value?.selectedIndex).isEqualTo(HeartScoreChartRange.Year.ordinal)
        assertThat(card.chart.value.series.single().points.map { it.y }).containsExactly(5.0)
    }

    @Test
    fun `without a history the chart is empty over the chosen range`() = runTest(UnconfinedTestDispatcher()) {
        // when
        val card = mapper.map(
            input = HeartScoreChartInput(
                component = HeartScoreComponent.Sleep,
                data = MutableStateFlow(null),
                selection = MutableStateFlow(HeartScoreChartRange.Fortnight),
                scope = backgroundScope,
                actionSink = sink,
            ),
        )

        // then
        val range = HeartScoreDetailFixtures.RANGE
        assertThat(card.chart.value.series).isEmpty()
        assertThat(card.chart.value.xAxis.range).isEqualTo(
            TimeCoordinates(origin = range.start).range(start = range.start, end = range.endExclusive),
        )
    }

    @Test
    fun `sleep tooltip expresses hours and minutes`() {
        // given
        val format = highlightFormat(component = HeartScoreComponent.Sleep)

        // when
        val highlight = format.label(point = CartesianChart.Point(x = 0.0, y = 8.5, xEnd = 86400.0))

        // then
        assertThat(highlight.value).isEqualTo(StringResource(MHCStrings.heart_detail_sleep_duration, 8, 30))
        assertThat(highlight.title).isEqualTo(StringResource("Dec 22, 2025"))
    }

    @Test
    fun `aggregate tooltip identifies the entire bucket`() {
        // when
        val highlight = highlightFormat(component = HeartScoreComponent.ExerciseMinutes)
            .label(point = CartesianChart.Point(x = 0.0, y = 45.0, xEnd = 172800.0))

        // then
        assertThat(highlight.title).isEqualTo(StringResource(MHCStrings.heart_detail_date_range, "Dec 22, 2025", "Dec 24, 2025"))
    }

    private fun highlightFormat(component: HeartScoreComponent) = HeartScoreHighlightFormat(
        coordinates = TimeCoordinates(origin = HeartScoreDetailFixtures.RANGE.start),
        zone = ZoneOffset.UTC,
        locale = Locale.US,
        component = component,
        kind = HeartScoreChartSeries.Kind.Value,
    )

    private fun input(
        scope: CoroutineScope,
        component: HeartScoreComponent,
        data: HeartScoreChartData,
    ) = HeartScoreChartInput(
        component = component,
        data = MutableStateFlow(data),
        selection = MutableStateFlow(HeartScoreChartRange.Fortnight),
        scope = scope,
        actionSink = sink,
    )

    private fun chart(content: ChartCard): CartesianChart = content.chart.value
}
