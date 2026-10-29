//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.achievements

import androidx.compose.ui.graphics.vector.ImageVector
import org.grovealliance.ui.StringResource

/**
 * A goal the participant unlocks by reaching it.
 *
 * @property icon the symbol standing for the goal
 * @property title the goal's name
 * @property description what reaching the goal takes
 * @property progress how close the participant is, from `0` for not started to `1` for unlocked
 */
data class Achievement(
    val icon: ImageVector,
    val title: StringResource,
    val description: StringResource,
    val progress: Float,
) {
    init {
        require(progress in 0f..1f) { "progress must lie within 0 and 1, but was $progress" }
    }

    /**
     * Whether the goal has been reached.
     */
    val isUnlocked: Boolean get() = progress >= 1f
}

/**
 * Achievements shown together under one [title].
 */
data class AchievementGroup(
    val title: StringResource,
    val achievements: List<Achievement>,
)
