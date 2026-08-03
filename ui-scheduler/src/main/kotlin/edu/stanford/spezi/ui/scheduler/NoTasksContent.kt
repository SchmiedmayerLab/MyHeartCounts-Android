//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui.scheduler

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.ImageResource
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Colors
import edu.stanford.spezi.ui.theme.Sizes
import edu.stanford.spezi.ui.theme.Spacings
import edu.stanford.spezi.ui.theme.SpeziTheme
import edu.stanford.spezi.ui.theme.TextStyles
import edu.stanford.spezi.ui.theme.ThemePreviews
import edu.stanford.spezi.ui.theme.bold

/**
 * The placeholder shown in place of a task list that has nothing to show.
 *
 * @param icon a symbol reinforcing the message
 * @param title the headline message
 * @param description an elaboration on the headline
 */
data class NoTasksContent(
    val icon: ImageResource,
    val title: StringResource,
    val description: StringResource? = null,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier.padding(Spacings.large),
            verticalArrangement = Arrangement.spacedBy(Spacings.small),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            icon.Content(modifier = Modifier.size(Sizes.Icon.medium))

            Text(
                text = title.text(),
                style = TextStyles.titleMedium.bold(),
                color = Colors.onSurface,
                textAlign = TextAlign.Center,
            )

            description?.let {
                Text(
                    text = it.text(),
                    style = TextStyles.bodyMedium,
                    color = Colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val content = NoTasksContent(
        icon = ImageResource(image = Icons.Default.Celebration),
        title = StringResource("You're All Set"),
        description = StringResource("All tasks have been completed!"),
    )

    SpeziTheme {
        content.Content(modifier = Modifier.fillMaxWidth())
    }
}
