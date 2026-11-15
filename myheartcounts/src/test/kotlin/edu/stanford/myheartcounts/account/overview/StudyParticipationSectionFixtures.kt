//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.outlined.MedicalInformation
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.ui.ToggleRow
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.ui.CardRow
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.ItemDisplayRow
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveSection

/**
 * Builds the account overview's study participation section, and the post-trial nudges section
 * below it; by default opted in to post-trial nudges.
 */
object StudyParticipationSectionFixtures {
    fun create() = groveSection(key = "study-participation") {
        card(key = "study-participation") {
            rows(
                listOf(
                    row(
                        key = "participation-stats",
                        icon = ImageResource(image = Icons.Default.MilitaryTech),
                        label = MHCStrings.account_view_participation_stats,
                    ),
                    row(
                        key = "comorbidities",
                        icon = ImageResource(image = Icons.Outlined.MedicalInformation),
                        label = MHCStrings.account_update_comorbidities,
                    ),
                    row(
                        key = "workout-preference",
                        icon = ImageResource(image = Icons.Default.FitnessCenter),
                        label = MHCStrings.account_workout_preference,
                    ),
                ),
            )
        }
    }

    fun createPostTrialNudges(
        isOptedIn: Boolean = true,
    ) = groveSection(key = "post-trial-nudges") {
        card(key = "post-trial-nudges") {
            row(
                key = "post-trial-nudges",
                content = ToggleRow(
                    title = StringResource(MHCStrings.account_post_trial_nudges_title),
                    subtitle = StringResource(MHCStrings.account_post_trial_nudges_subtitle),
                    isOn = MutableStateFlow(value = isOptedIn),
                    onToggle = {},
                ),
            )
        }
    }

    private fun row(
        key: String,
        icon: ImageResource,
        label: Int,
    ) = CardRow(
        key = key,
        content = ItemDisplayRow(
            leadingImage = icon,
            label = StringResource(label),
            value = null,
            showArrow = true,
            onClick = {},
        ),
    )
}
