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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import edu.stanford.spezi.ui.AsyncTextButton
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.ImageResource
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Colors
import edu.stanford.spezi.ui.theme.Spacings
import edu.stanford.spezi.ui.theme.SpeziTheme
import edu.stanford.spezi.ui.theme.TextStyles
import edu.stanford.spezi.ui.theme.ThemePreviews
import edu.stanford.spezi.ui.theme.medium

/**
 * A group of tasks sharing a heading, such as all tasks falling on the same day.
 *
 * @param title what the group has in common, already formatted for display; omitted for ungrouped lists
 * @param tiles the tasks in the group, in display order
 * @param subtitle further detail about the group, shown beneath [title]
 */
data class TaskSection(
    val title: StringResource?,
    val tiles: List<TaskTile>,
    val subtitle: StringResource? = null,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Spacings.small),
        ) {
            title?.let {
                Text(
                    text = it.text(),
                    style = TextStyles.titleSmall.medium(),
                    color = Colors.onSurfaceVariant,
                )
            }

            subtitle?.let {
                Text(
                    text = it.text(),
                    style = TextStyles.bodySmall,
                    color = Colors.onSurfaceVariant,
                )
            }

            tiles.forEach {
                it.Content(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val section = TaskSection(
        title = StringResource("August 1, 2026"),
        tiles = listOf(
            TaskTile(
                header = TaskTileHeader(
                    icon = ImageResource(image = Icons.AutoMirrored.Filled.Assignment),
                    title = StringResource("Daily Check-In"),
                    categoryLabel = StringResource("Questionnaire"),
                    timing = StringResource("9:00 AM"),
                ),
                instructions = StringResource("Tell us how you have been feeling over the past 24 hours."),
                action = AsyncTextButton(
                    title = StringResource("Answer Survey"),
                    action = {},
                ),
            ),
            TaskTile(
                header = TaskTileHeader(
                    icon = ImageResource(image = Icons.AutoMirrored.Filled.DirectionsWalk),
                    title = StringResource("6-Minute Walk Test"),
                    categoryLabel = StringResource("Active Task"),
                ),
                instructions = StringResource("Walk at a comfortable pace for six minutes."),
                action = AsyncTextButton(
                    title = StringResource("Take Test"),
                    action = {},
                ),
            ),
        ),
    )

    SpeziTheme {
        section.Content(modifier = Modifier.fillMaxWidth())
    }
}
