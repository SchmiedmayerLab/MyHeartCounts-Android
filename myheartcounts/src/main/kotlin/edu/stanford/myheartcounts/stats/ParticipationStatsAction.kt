//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.stats

/**
 * An intent raised by the participation stats screen.
 */
sealed interface ParticipationStatsAction {

    /**
     * The participant closed the screen.
     */
    data object Close : ParticipationStatsAction

    /**
     * The participant asked what the stats cover.
     */
    data object InfoSelected : ParticipationStatsAction

    /**
     * The participant went back from the page on screen to the stats.
     */
    data object Back : ParticipationStatsAction

    /**
     * The participant opened the list of all achievements.
     */
    data object AchievementsSelected : ParticipationStatsAction
}
