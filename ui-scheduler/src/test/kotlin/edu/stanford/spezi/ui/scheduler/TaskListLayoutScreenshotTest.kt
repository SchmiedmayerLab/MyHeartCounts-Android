//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui.scheduler

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.ui.Modifier
import edu.stanford.spezi.testing.screenshot.ScreenshotTest
import edu.stanford.spezi.ui.AsyncTextButton
import edu.stanford.spezi.ui.ImageResource
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Spacings
import org.junit.Test

class TaskListLayoutScreenshotTest : ScreenshotTest() {

    @Test
    fun `TaskListLayout populated screenshot`() {
        val layout = TaskListLayout(
            header = TaskListHeader(
                title = StringResource("Today's Tasks"),
            ),
            sections = listOf(
                TaskSection(
                    title = null,
                    tiles = listOf(checkInTile()),
                ),
            ),
        )

        screenshot {
            layout.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }

    @Test
    fun `TaskListLayout grouped by day screenshot`() {
        val layout = TaskListLayout(
            header = TaskListHeader(
                title = StringResource("Next 2 Weeks"),
                subtitle = StringResource("01/08/2026 – 15/08/2026"),
            ),
            sections = listOf(
                TaskSection(
                    title = StringResource("August 1, 2026"),
                    tiles = listOf(checkInTile()),
                ),
                TaskSection(
                    title = StringResource("August 2, 2026"),
                    tiles = listOf(checkInTile()),
                ),
            ),
        )

        screenshot {
            layout.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }

    @Test
    fun `TaskListLayout empty screenshot`() {
        val layout = TaskListLayout(
            header = TaskListHeader(
                title = StringResource("Today's Tasks"),
            ),
            emptyState = NoTasksContent(
                icon = ImageResource(image = Icons.Default.Celebration),
                title = StringResource("You're All Set"),
                description = StringResource("All tasks have been completed!"),
            ),
        )

        screenshot {
            layout.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }

    @Test
    fun `TaskListLayout with additional sections screenshot`() {
        val layout = TaskListLayout(
            header = TaskListHeader(
                title = StringResource("Today's Tasks"),
            ),
            sections = listOf(
                TaskSection(
                    title = null,
                    tiles = listOf(checkInTile()),
                ),
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

        screenshot {
            layout.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }

    private fun checkInTile() = TaskTile(
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
    )
}
