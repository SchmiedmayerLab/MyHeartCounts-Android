//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import edu.stanford.myheartcounts.dashboard.HeartScoreMeasurement
import edu.stanford.myheartcounts.dashboard.HeartScoreResult
import edu.stanford.myheartcounts.dashboard.ui.chart.CartesianChart
import edu.stanford.myheartcounts.dashboard.ui.chart.ChartCard
import edu.stanford.myheartcounts.dashboard.ui.chart.cartesianChart
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.scheduler.InstantRange
import org.grovealliance.ui.StringResource
import java.time.Instant

object HeartScoreDetailFixtures {
    val NOW: Instant = Instant.parse("2026-01-04T12:00:00Z")
    val RANGE = InstantRange(
        start = Instant.parse("2025-12-22T00:00:00Z"),
        endExclusive = Instant.parse("2026-01-05T00:00:00Z"),
    )

    fun result(component: HeartScoreComponent, value: Double?) = HeartScoreResult(
        component = component,
        score = value?.let { 0.8 },
        measuredAt = value?.let { NOW },
        measurement = value?.let { HeartScoreMeasurement.Quantity(value = it) },
        usesAsianBodyMassIndex = false,
    )

    fun chart(points: List<HeartScoreChartPoint>) = HeartScoreChartData(
        timeRange = RANGE,
        series = listOf(HeartScoreChartSeries(
            kind = HeartScoreChartSeries.Kind.Value,
            points = points,
        )),
    )

    fun chartCard() = ChartCard(
        title = StringResource("Recent Values"),
        picker = MutableStateFlow(null),
        chart = MutableStateFlow(
            cartesianChart(
                emptyText = StringResource("No Data"),
                description = StringResource("History"),
                accessibilityLabels = CartesianChart.AccessibilityLabels(
                    nextPoint = StringResource("Next point"),
                    previousPoint = StringResource("Previous point"),
                ),
            ) {},
        ),
    )
}
