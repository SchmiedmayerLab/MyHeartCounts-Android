//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.scheduler

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveIconButtonComposable
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.bold
import org.grovealliance.ui.tinted

/**
 * A single action the participant is being prompted to take.
 *
 * A completed action keeps its title and message but drops its [action] button.
 *
 * @param icon a symbol representing the action; replaced by a confirmation mark once completed
 * @param title what the action is
 * @param message why the participant is being asked to take it
 * @param action the button performing the action; omitted once completed
 * @param isCompleted whether the action has already been taken
 * @param onStopSuggesting stops the action from being suggested again; omitted when it cannot be
 * dismissed. Any confirmation is the caller's to present.
 */
data class PromptedActionRow(
    val icon: ImageResource,
    val title: StringResource,
    val message: StringResource,
    val action: AsyncTextButton? = null,
    val isCompleted: Boolean = false,
    val onStopSuggesting: (() -> Unit)? = null,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Box(modifier = modifier) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacings.medium),
                verticalArrangement = Arrangement.spacedBy(Spacings.small),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacings.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Badge()
                    Text(
                        modifier = Modifier.weight(1f),
                        text = title.text(),
                        style = TextStyles.titleMedium.bold(),
                        color = Colors.onSurface,
                    )
                }

                Text(
                    text = message.text(),
                    style = TextStyles.bodyMedium,
                    color = Colors.onSurfaceVariant,
                )

                if (!isCompleted) {
                    action?.Content(modifier = Modifier.fillMaxWidth())
                }
            }

            StopSuggestingButton(modifier = Modifier.align(Alignment.TopEnd))
        }
    }

    /**
     * The action's symbol, or a confirmation mark once it has been taken.
     */
    @Composable
    private fun Badge() {
        val shape: Shape = if (isCompleted) GroveShapes.circle else GroveShapes.medium
        val badgeIcon = if (isCompleted) remember { ImageResource(image = Icons.Default.Check) } else icon

        Box(
            modifier = Modifier
                .size(BADGE_SIZE)
                .background(color = Colors.primary, shape = shape),
            contentAlignment = Alignment.Center,
        ) {
            badgeIcon.tinted(tint = Colors.onPrimary).Content(modifier = Modifier.size(Sizes.Icon.small))
        }
    }

    /**
     * The affordance stopping the action from being suggested again.
     */
    @Composable
    private fun StopSuggestingButton(modifier: Modifier) {
        val onClick = onStopSuggesting ?: return
        val image = remember { ImageResource(image = Icons.Default.Close, tint = { Colors.onSurfaceVariant }) }
        GroveIconButtonComposable(
            modifier = modifier,
            image = image,
            onClick = onClick,
        )
    }

    private companion object {
        val BADGE_SIZE = 36.dp
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val pending = PromptedActionRow(
        icon = ImageResource(image = Icons.Default.Notifications),
        title = StringResource("Enable Notifications"),
        message = StringResource("We will remind you when a new task becomes available."),
        action = AsyncTextButton(
            title = StringResource("Enable"),
            action = {},
        ),
        isCompleted = false,
        onStopSuggesting = {},
    )

    val completed = pending.copy(isCompleted = true)

    GroveTheme {
        Column(
            modifier = Modifier.padding(Spacings.medium),
            verticalArrangement = Arrangement.spacedBy(Spacings.medium),
        ) {
            pending.Content(modifier = Modifier.fillMaxWidth())
            completed.Content(modifier = Modifier.fillMaxWidth())
        }
    }
}
