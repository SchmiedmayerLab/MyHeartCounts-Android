//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui.scheduler

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.ui.Modifier
import edu.stanford.spezi.testing.screenshot.ScreenshotTest
import edu.stanford.spezi.ui.AsyncTextButton
import edu.stanford.spezi.ui.ImageResource
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Spacings
import org.junit.Test

class TaskTileScreenshotTest : ScreenshotTest() {

    @Test
    fun `TaskTile pending screenshot`() {
        val tile = TaskTile(
            header = header(),
            instructions = INSTRUCTIONS,
            action = AsyncTextButton(
                title = StringResource("Answer Survey"),
                action = {},
            ),
        )

        screenshot {
            tile.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }

    @Test
    fun `TaskTile disabled action screenshot`() {
        val tile = TaskTile(
            header = header(),
            instructions = INSTRUCTIONS,
            action = AsyncTextButton(
                title = StringResource("Answer Survey"),
                enabled = false,
                action = {},
            ),
        )

        screenshot {
            tile.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }

    @Test
    fun `TaskTile without action screenshot`() {
        val tile = TaskTile(
            header = header(),
            instructions = INSTRUCTIONS,
        )

        screenshot {
            tile.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }

    @Test
    fun `TaskTile with more info screenshot`() {
        val tile = TaskTile(
            header = header(),
            instructions = INSTRUCTIONS,
            action = AsyncTextButton(
                title = StringResource("Answer Survey"),
                action = {},
            ),
            onMoreInfoClick = {},
        )

        screenshot {
            tile.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }

    @Test
    fun `TaskTile completed screenshot`() {
        val tile = TaskTile(
            header = header(),
            instructions = INSTRUCTIONS,
            isCompleted = true,
        )

        screenshot {
            tile.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }

    @Test
    fun `TaskTile completed repeatable screenshot`() {
        val tile = TaskTile(
            header = header(),
            instructions = INSTRUCTIONS,
            action = AsyncTextButton(
                title = StringResource("Answer Survey Again"),
                action = {},
            ),
            isCompleted = true,
        )

        screenshot {
            tile.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }

    private fun header() = TaskTileHeader(
        icon = ImageResource(image = Icons.AutoMirrored.Filled.Assignment),
        title = StringResource("Daily Check-In"),
        categoryLabel = StringResource("Questionnaire"),
        timing = StringResource("9:00 AM"),
    )

    private companion object {
        val INSTRUCTIONS = StringResource("Tell us how you have been feeling over the past 24 hours.")
    }
}
