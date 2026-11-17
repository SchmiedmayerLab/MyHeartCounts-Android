//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.task.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.MonitorHeart
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveSections
import org.grovealliance.ui.scheduler.NoTasksContent
import org.grovealliance.ui.scheduler.TaskListHeader
import org.grovealliance.ui.scheduler.TaskTile
import org.grovealliance.ui.scheduler.TaskTileHeader
import org.junit.Test

class TaskListLayoutScreenshotTest : ScreenshotTest() {
    private val checkIn = TaskTile(
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
    private val ecg = TaskTile(
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
    )
    private val base = TaskListLayout(
        header = TaskListHeader(title = StringResource("Today's Tasks")),
        sections = groveSections {
            section(key = "today") { content(key = "check-in", content = checkIn) }
        },
    )

    @Test
    fun `TaskListLayout populated screenshot`() {
        // when / then
        screenshot(layout = base)
    }

    @Test
    fun `TaskListLayout grouped by day screenshot`() {
        // given
        val layout = base.copy(
            header = TaskListHeader(
                title = StringResource("Next 2 Weeks"),
                subtitle = StringResource("01/01/2026 – 15/01/2026"),
            ),
            sections = groveSections {
                listOf("January 1, 2026", "January 2, 2026").forEach { day ->
                    section(key = day) {
                        title(day)
                        content(key = "check-in", content = checkIn)
                        content(key = "ecg", content = ecg)
                    }
                }
            },
        )

        // when / then
        screenshot(layout = layout)
    }

    @Test
    fun `TaskListLayout empty screenshot`() {
        // given
        val layout = base.copy(
            sections = emptyList(),
            emptyState = NoTasksContent(
                icon = ImageResource(image = Icons.Default.Celebration),
                title = StringResource("You're All Set"),
                description = StringResource("All tasks have been completed!"),
            ),
        )

        // when / then
        screenshot(layout = layout)
    }

    @Test
    fun `TaskListLayout with additional sections screenshot`() {
        // given
        val layout = base.copy(
            additionalSections = groveSections {
                section(key = "always-available") {
                    title("Always Available")
                    subtitle("Take these whenever it suits you.")
                    content(key = "ecg", content = ecg)
                }
            },
        )

        // when / then
        screenshot(layout = layout)
    }

    @Test
    fun `TaskListLayout with header and section subtitles screenshot`() {
        // given
        val layout = base.copy(
            header = TaskListHeader(
                title = StringResource("Next 2 Weeks"),
                subtitle = StringResource("01/01/2026 – 15/01/2026"),
            ),
            sections = groveSections {
                section(key = "january-1") {
                    title("January 1, 2026")
                    content(key = "check-in", content = checkIn)
                }
                section(key = "january-2") {
                    title("January 2, 2026")
                    content(key = "check-in", content = checkIn)
                }
            },
            additionalSections = groveSections {
                section(key = "always-available") {
                    title("Always Available")
                    subtitle("Take these whenever it suits you.")
                    content(key = "ecg", content = ecg)
                }
            },
        )

        // when / then
        screenshot(layout = layout)
    }

    private fun screenshot(layout: TaskListLayout) {
        screenshot { layout.Content() }
    }
}
