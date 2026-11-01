//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import edu.stanford.myheartcounts.dashboard.ui.chart.ChartCard

class FakeHeartScoreChartMapper : HeartScoreChartMapper {
    val inputs = mutableListOf<HeartScoreChartInput>()
    override fun map(input: HeartScoreChartInput): ChartCard {
        inputs += input
        return HeartScoreDetailFixtures.chartCard()
    }
}
