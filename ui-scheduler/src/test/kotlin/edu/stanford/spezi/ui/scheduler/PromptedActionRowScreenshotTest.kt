//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui.scheduler

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.Modifier
import edu.stanford.spezi.testing.screenshot.ScreenshotTest
import edu.stanford.spezi.ui.AsyncTextButton
import edu.stanford.spezi.ui.ImageResource
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Spacings
import org.junit.Test

class PromptedActionRowScreenshotTest : ScreenshotTest() {

    @Test
    fun `PromptedActionRow pending screenshot`() {
        val row = PromptedActionRow(
            icon = ImageResource(image = Icons.Default.Notifications),
            title = StringResource("Enable Notifications"),
            message = MESSAGE,
            action = AsyncTextButton(
                title = StringResource("Enable"),
                action = {},
            ),
            onStopSuggesting = {},
        )

        screenshot {
            row.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }

    @Test
    fun `PromptedActionRow not dismissible screenshot`() {
        val row = PromptedActionRow(
            icon = ImageResource(image = Icons.Default.Notifications),
            title = StringResource("Enable Notifications"),
            message = MESSAGE,
            action = AsyncTextButton(
                title = StringResource("Enable"),
                action = {},
            ),
        )

        screenshot {
            row.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }

    @Test
    fun `PromptedActionRow completed screenshot`() {
        val row = PromptedActionRow(
            icon = ImageResource(image = Icons.Default.Notifications),
            title = StringResource("Enable Notifications"),
            message = MESSAGE,
            action = AsyncTextButton(
                title = StringResource("Enable"),
                action = {},
            ),
            isCompleted = true,
        )

        screenshot {
            row.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }

    private companion object {
        val MESSAGE = StringResource("We will remind you when a new task becomes available.")
    }
}
