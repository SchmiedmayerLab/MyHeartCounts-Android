//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui.scheduler

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.SpeziCard
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Colors
import edu.stanford.spezi.ui.theme.Spacings
import edu.stanford.spezi.ui.theme.SpeziTheme
import edu.stanford.spezi.ui.theme.TextStyles
import edu.stanford.spezi.ui.theme.ThemePreviews
import edu.stanford.spezi.ui.theme.bold

/**
 * A short encouragement shown above the participant's tasks.
 *
 * @param title the headline of the encouragement
 * @param message the body of the encouragement
 */
data class NudgeBanner(
    val title: StringResource,
    val message: StringResource,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        SpeziCard(
            modifier = modifier,
            containerColor = Colors.secondaryContainer,
        ) {
            Column(
                modifier = Modifier.padding(Spacings.medium),
                verticalArrangement = Arrangement.spacedBy(Spacings.extraSmall),
            ) {
                Text(
                    text = title.text(),
                    style = TextStyles.titleMedium.bold(),
                    color = Colors.onSecondaryContainer,
                )
                Text(
                    text = message.text(),
                    style = TextStyles.bodyMedium,
                    color = Colors.onSecondaryContainer,
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val banner = NudgeBanner(
        title = StringResource("Keep it up!"),
        message = StringResource("You have walked more this week than last week. A short walk today keeps the streak going."),
    )

    SpeziTheme {
        banner.Content(modifier = Modifier.fillMaxWidth())
    }
}
