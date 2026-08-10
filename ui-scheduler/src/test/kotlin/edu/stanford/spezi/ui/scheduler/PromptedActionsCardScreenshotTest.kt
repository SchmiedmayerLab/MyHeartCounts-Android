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
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.Modifier
import edu.stanford.spezi.testing.screenshot.ScreenshotTest
import edu.stanford.spezi.ui.ImageResource
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Spacings
import org.junit.Test

class PromptedActionsCardScreenshotTest : ScreenshotTest() {

    @Test
    fun `PromptedActionsCard single action screenshot`() {
        val card = PromptedActionsCard(
            icons = listOf(ImageResource(image = Icons.Default.Notifications)),
            title = StringResource("Finish setting up"),
            subtitle = StringResource("1 step remaining"),
            onClick = {},
        )

        screenshot {
            card.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }

    @Test
    fun `PromptedActionsCard cluster screenshot`() {
        val card = PromptedActionsCard(
            icons = listOf(
                ImageResource(image = Icons.Default.Notifications),
                ImageResource(image = Icons.Default.MonitorHeart),
                ImageResource(image = Icons.AutoMirrored.Filled.Assignment),
            ),
            title = StringResource("Finish setting up"),
            subtitle = StringResource("3 steps remaining"),
            onClick = {},
        )

        screenshot {
            card.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }

    @Test
    fun `PromptedActionsCard overflow screenshot`() {
        val card = PromptedActionsCard(
            icons = listOf(
                ImageResource(image = Icons.Default.Notifications),
                ImageResource(image = Icons.Default.MonitorHeart),
                ImageResource(image = Icons.AutoMirrored.Filled.Assignment),
                ImageResource(image = Icons.AutoMirrored.Filled.DirectionsWalk),
            ),
            title = StringResource("Finish setting up"),
            subtitle = StringResource("4 steps remaining"),
            onClick = {},
        )

        screenshot {
            card.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }

    @Test
    fun `PromptedActionsCard settled screenshot`() {
        val card = PromptedActionsCard(
            icons = emptyList(),
            title = StringResource("You're all set"),
            subtitle = StringResource("Nothing left to set up"),
        )

        screenshot {
            card.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }
}
