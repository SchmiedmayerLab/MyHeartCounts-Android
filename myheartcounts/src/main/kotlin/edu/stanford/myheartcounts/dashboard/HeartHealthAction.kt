//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

/**
 * An intent raised by the Heart Health tab.
 */
sealed interface HeartHealthAction {

    /**
     * The participant opened the details of [component].
     */
    data class ComponentSelected(val component: HeartScoreComponent) : HeartHealthAction

    /**
     * The participant opened their past timed walk/run test results.
     */
    data object PastDataSelected : HeartHealthAction
}
