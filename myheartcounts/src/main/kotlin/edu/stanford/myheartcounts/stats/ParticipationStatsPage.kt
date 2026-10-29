//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.stats

/**
 * A page of the participation stats sheet. Pages declared later open on top of [Stats]: moving to a
 * later page slides forward, moving to an earlier one slides back.
 */
enum class ParticipationStatsPage {
    Stats,
    Explainer,
    Achievements,
}
