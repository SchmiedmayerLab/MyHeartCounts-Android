//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.scheduler

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.ui.Modifier
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Spacings
import org.junit.Test

class TaskSectionScreenshotTest : ScreenshotTest() {

    @Test
    fun `TaskSection with title screenshot`() {
        val section = TaskSection(
            title = StringResource("August 1, 2026"),
            tiles = tiles(),
        )

        screenshot {
            section.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }

    @Test
    fun `TaskSection without title screenshot`() {
        val section = TaskSection(
            title = null,
            tiles = tiles(),
        )

        screenshot {
            section.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }

    private fun tiles() = listOf(
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
    )
}
