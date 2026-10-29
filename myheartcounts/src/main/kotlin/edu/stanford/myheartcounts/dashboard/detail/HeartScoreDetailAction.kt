//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

/**
 * What the participant can do on a factor's details screen.
 */
sealed interface HeartScoreDetailAction {

    /**
     * Dismiss the details sheet.
     */
    data object Close : HeartScoreDetailAction

    /**
     * Read the details again after a failure.
     */
    data object Retry : HeartScoreDetailAction

    /**
     * Enter a new measurement or answer the component’s questionnaire.
     */
    data object AddData : HeartScoreDetailAction

    /**
     * Show the chart over [range].
     */
    data class RangeSelected(val range: HeartScoreChartRange) : HeartScoreDetailAction

    /**
     * Open one of the explanation's links.
     */
    data class LinkSelected(val url: String) : HeartScoreDetailAction
}
