//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.internal

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.dashboard.ui.DashboardColorPalette
import edu.stanford.myheartcounts.dashboard.ui.chart.CartesianChart
import edu.stanford.myheartcounts.dashboard.ui.chart.Highlight
import edu.stanford.myheartcounts.dashboard.ui.chart.HighlightFormat
import org.grovealliance.ui.StringResource
import org.junit.Test

class ChartHighlightTest {
    private val start = 0.0
    private val plot = ChartPlot(
        area = Rect(left = 0f, top = 0f, right = 100f, bottom = 100f),
        xRange = CartesianChart.Range(start = 0.0, end = 100.0),
        yRange = CartesianChart.Range(start = 0.0, end = 10.0),
    )
    private val base = CartesianChart.Series(
        style = CartesianChart.Style.Line,
        color = DashboardColorPalette.red,
        points = listOf(point(from = 10, to = 20), point(from = 40, to = 40)),
        highlightFormat = Format,
    )

    @Test
    fun `snapping uses spans and chooses the first equally near candidate`() {
        val candidates = highlightCandidates(series = listOf(base), visiblePoints = listOf(base.points))
        assertThat(candidates.nearestTo(x = 18f, plot = plot)).isEqualTo(0)
        assertThat(candidates.nearestTo(x = 30f, plot = plot)).isEqualTo(0)
        assertThat(candidates.nearestTo(x = 39f, plot = plot)).isEqualTo(1)
        assertThat(candidates.nearestTo(x = -20f, plot = plot)).isEqualTo(0)
        assertThat(candidates.nearestTo(x = 120f, plot = plot)).isEqualTo(1)
        assertThat(emptyList<ChartCandidate>().nearestTo(x = 50f, plot = plot)).isNull()
    }

    @Test
    fun `candidates exclude unformatted series and follow time order across series`() {
        val earlier = base.copy(points = listOf(point(from = 0, to = 0)))
        val hidden = base.copy(highlightFormat = null)
        val series = listOf(base, hidden, earlier)
        val candidates = highlightCandidates(series = series, visiblePoints = series.map { it.points })
        assertThat(candidates.map { it.series }).containsExactly(2, 0, 0).inOrder()
        assertThat(candidates.map { it.middle }).containsExactly(
            start, (start + 15), (start + 40),
        ).inOrder()
    }

    @Test
    fun `selection steps from either end and stops at the boundary`() {
        assertThat(stepSelection(selected = null, delta = 1, count = 3)).isEqualTo(0)
        assertThat(stepSelection(selected = null, delta = -1, count = 3)).isEqualTo(2)
        assertThat(stepSelection(selected = 0, delta = -1, count = 3)).isEqualTo(0)
        assertThat(stepSelection(selected = 2, delta = 1, count = 3)).isEqualTo(2)
        assertThat(stepSelection(selected = 0, delta = 1, count = 3)).isEqualTo(1)
        assertThat(stepSelection(selected = 2, delta = -1, count = 3)).isEqualTo(1)
    }

    @Test
    fun `blood pressure highlights both series at the same moment in series order`() {
        val systolic = point(from = 40, to = 40).copy(y = 128.0)
        val diastolic = systolic.copy(y = 80.0)
        val series = listOf(base.copy(points = listOf(systolic)), base.copy(points = listOf(diastolic)))
        val highlights = highlightsAt(
            anchor = ChartCandidate(series = 1, point = diastolic),
            series = series,
            visiblePoints = series.map { it.points },
            colors = listOf(Color.Red, Color.Blue),
        )
        assertThat(highlights).containsExactly(
            Color.Red to Format.label(systolic), Color.Blue to Format.label(diastolic),
        ).inOrder()
    }

    @Test
    fun `matching excludes missing and unformatted series and the exclusive span end`() {
        val anchor = point(from = 20, to = 20)
        val series = listOf(
            base.copy(points = listOf(anchor)),
            base.copy(points = listOf(point(from = 0, to = 20))),
            base.copy(points = listOf(point(from = 20, to = 30))),
            base.copy(highlightFormat = null),
            base.copy(points = emptyList()),
        )
        val highlights = highlightsAt(
            anchor = ChartCandidate(series = 0, point = anchor),
            series = series,
            visiblePoints = series.map { it.points },
            colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Gray, Color.Black),
        )
        assertThat(highlights.map { it.first }).containsExactly(Color.Red, Color.Green).inOrder()
    }

    @Test
    fun `range excludes a span ending at its start but includes an instant there`() {
        val range = CartesianChart.Range(start = start, end = (start + 100))
        assertThat(point(from = -10, to = 0).overlaps(range)).isFalse()
        assertThat(point(from = 0, to = 0).overlaps(range)).isTrue()
        assertThat(point(from = -10, to = 1).overlaps(range)).isTrue()
        assertThat(point(from = 100, to = 100).overlaps(range)).isTrue()
    }

    @Test
    fun `range excludes a span starting at its end but includes an instant there`() {
        // given
        val range = CartesianChart.Range(start = start, end = (start + 100))

        // when / then
        assertThat(point(from = 100, to = 110).overlaps(range)).isFalse()
        assertThat(point(from = 99, to = 110).overlaps(range)).isTrue()
        assertThat(point(from = 100, to = 100).overlaps(range)).isTrue()
    }

    private fun point(from: Long, to: Long): CartesianChart.Point = CartesianChart.Point(
        x = (start + from),
        xEnd = (start + to),
        y = 8.0,
    )

    private object Format : HighlightFormat {
        override fun label(point: CartesianChart.Point): Highlight = Highlight(
            value = StringResource("${point.y} mmHg"),
            title = StringResource("11 Oct"),
        )
    }
}
