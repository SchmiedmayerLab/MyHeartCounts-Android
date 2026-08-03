//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui.scheduler

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import edu.stanford.spezi.resources.Strings
import edu.stanford.spezi.ui.AsyncTextButton
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.ImageResource
import edu.stanford.spezi.ui.SpeziCard
import edu.stanford.spezi.ui.SpeziIconButtonComposable
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Colors
import edu.stanford.spezi.ui.theme.Sizes
import edu.stanford.spezi.ui.theme.Spacings
import edu.stanford.spezi.ui.theme.SpeziTheme
import edu.stanford.spezi.ui.theme.TextStyles
import edu.stanford.spezi.ui.theme.ThemePreviews
import edu.stanford.spezi.ui.theme.medium

/**
 * A single task presented to the participant.
 *
 * A completed task collapses to its title alone; its instructions and action are shown only while
 * it is still open, or when it may be performed again.
 *
 * @param header what the task is and when it is due
 * @param instructions what the participant is asked to do
 * @param action the button performing the task; omitted when the task cannot currently be performed
 * @param isCompleted whether the task has already been completed
 * @param onMoreInfoClick opens further explanation of the task; omitted when there is none
 */
data class TaskTile(
    val header: TaskTileHeader,
    val instructions: StringResource? = null,
    val action: AsyncTextButton? = null,
    val isCompleted: Boolean = false,
    val onMoreInfoClick: (() -> Unit)? = null,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        SpeziCard(modifier = modifier) {
            Column(
                modifier = Modifier.padding(Spacings.medium),
                verticalArrangement = Arrangement.spacedBy(Spacings.small),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (isCompleted) {
                            CompletedHeader()
                        } else {
                            header.Content(modifier = Modifier.fillMaxWidth())
                        }
                    }
                    MoreInfoButton()
                }

                if (instructions != null || action != null) {
                    HorizontalDivider()
                }

                instructions?.let {
                    Text(
                        text = it.text(),
                        style = TextStyles.bodyMedium,
                        color = Colors.onSurface,
                    )
                }

                action?.Content(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    /**
     * The condensed heading shown in place of [header] once the task has been completed.
     */
    @Composable
    private fun CompletedHeader() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacings.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = COMPLETED_ICON_COLOR,
                modifier = Modifier.size(Sizes.Icon.medium),
            )
            Column {
                Text(
                    text = header.title.text(),
                    style = TextStyles.titleMedium.medium(),
                    color = Colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = StringResource(Strings.task_completed).text(),
                    style = TextStyles.bodyMedium,
                    color = Colors.onSurfaceVariant,
                )
            }
        }
    }

    /**
     * The affordance opening further explanation of the task.
     */
    @Composable
    private fun MoreInfoButton() {
        val onClick = onMoreInfoClick ?: return
        val image = remember { ImageResource(image = Icons.Default.Info) }
        SpeziIconButtonComposable(
            image = image,
            onClick = onClick,
        )
    }

    private companion object {
        /**
         * The color marking a task as done, which the Material color scheme has no role for.
         */
        val COMPLETED_ICON_COLOR = Color(0xFF34C759)
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val header = TaskTileHeader(
        icon = ImageResource(image = Icons.AutoMirrored.Filled.Assignment),
        title = StringResource("Daily Check-In"),
        categoryLabel = StringResource("Questionnaire"),
        timing = StringResource("9:00 AM"),
    )

    val open = TaskTile(
        header = header,
        instructions = StringResource("Tell us how you have been feeling over the past 24 hours."),
        action = AsyncTextButton(
            title = StringResource("Answer Survey"),
            action = {},
        ),
        isCompleted = false,
        onMoreInfoClick = {},
    )

    val disabled = TaskTile(
        header = header,
        instructions = StringResource("Tell us how you have been feeling over the past 24 hours."),
        action = AsyncTextButton(
            title = StringResource("Answer Survey"),
            enabled = false,
            action = {},
        ),
        isCompleted = false,
    )

    val completed = TaskTile(
        header = header,
        instructions = StringResource("Tell us how you have been feeling over the past 24 hours."),
        action = null,
        isCompleted = true,
    )

    SpeziTheme {
        Column(
            modifier = Modifier.padding(Spacings.medium),
            verticalArrangement = Arrangement.spacedBy(Spacings.medium),
        ) {
            open.Content(modifier = Modifier.fillMaxWidth())
            disabled.Content(modifier = Modifier.fillMaxWidth())
            completed.Content(modifier = Modifier.fillMaxWidth())
        }
    }
}
