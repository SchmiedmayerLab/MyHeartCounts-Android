//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.ImageResource
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Spacings
import edu.stanford.spezi.ui.theme.SpeziTheme
import edu.stanford.spezi.ui.theme.ThemePreviews

/**
 * Displays the list of informational rows for an onboarding page.
 *
 * @property areas Ordered information rows shown in the body.
 */
data class OnboardingInformation(
    val areas: List<OnboardingArea>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Spacings.large),
        ) {
            areas.forEach { area -> area.Content() }
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    SpeziTheme {
        OnboardingInformation(
            areas = listOf(
                OnboardingArea(
                    icon = ImageResource(image = Icons.Default.Favorite),
                    title = StringResource("Heart Health"),
                    description = StringResource("Track your heart rate and activity patterns."),
                ),
                OnboardingArea(
                    icon = ImageResource(image = Icons.Default.Star),
                    title = StringResource("Research Impact"),
                    description = StringResource("Contribute to groundbreaking cardiovascular research."),
                ),
            )
        ).Content()
    }
}
