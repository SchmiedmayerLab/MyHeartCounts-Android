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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import edu.stanford.spezi.ui.AsyncTextButton
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.ImageResource
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Spacings
import edu.stanford.spezi.ui.theme.SpeziTheme
import edu.stanford.spezi.ui.theme.ThemePreviews

/**
 * A list of the participant's tasks, optionally grouped into sections.
 *
 * The layout does not scroll; it is meant to be placed inside a scroll container owned by the
 * surrounding screen, alongside whatever else that screen shows.
 *
 * @param header what the list covers
 * @param sections the task groups, in display order; when empty the [emptyState] takes their place
 * @param emptyState what to show instead of an empty list of [sections]
 * @param additionalSections groups always shown below the list, such as tasks available at any time
 */
data class TaskListLayout(
    val header: TaskListHeader? = null,
    val sections: List<TaskSection> = emptyList(),
    val emptyState: NoTasksContent? = null,
    val additionalSections: List<TaskSection> = emptyList(),
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Spacings.large),
        ) {
            header?.Content(modifier = Modifier.fillMaxWidth())

            if (sections.isEmpty()) {
                emptyState?.Content(modifier = Modifier.fillMaxWidth())
            } else {
                sections.forEach {
                    it.Content(modifier = Modifier.fillMaxWidth())
                }
            }

            additionalSections.forEach {
                it.Content(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val layout = TaskListLayout(
        header = TaskListHeader(
            title = StringResource("Today's Tasks"),
        ),
        sections = listOf(
            TaskSection(
                title = null,
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
                ),
            ),
        ),
        emptyState = NoTasksContent(
            icon = ImageResource(image = Icons.Default.Celebration),
            title = StringResource("You're All Set"),
            description = StringResource("All tasks have been completed!"),
        ),
        additionalSections = listOf(
            TaskSection(
                title = StringResource("Always Available"),
                tiles = listOf(
                    TaskTile(
                        header = TaskTileHeader(
                            icon = ImageResource(image = Icons.Default.MonitorHeart),
                            title = StringResource("Electrocardiogram"),
                            categoryLabel = StringResource("Active Task"),
                        ),
                        instructions = StringResource("Record a short ECG with a connected wearable device."),
                        action = AsyncTextButton(
                            title = StringResource("Take ECG"),
                            action = {},
                        ),
                    ),
                ),
            ),
        ),
    )

    SpeziTheme {
        layout.Content(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacings.medium)
        )
    }
}
