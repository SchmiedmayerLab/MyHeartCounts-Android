//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui.scheduler

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.ui.Modifier
import edu.stanford.spezi.testing.screenshot.ScreenshotTest
import edu.stanford.spezi.ui.ImageResource
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Spacings
import org.junit.Test

class TaskActionsMenuScreenshotTest : ScreenshotTest() {

    @Test
    fun `TaskActionsMenu collapsed screenshot`() {
        val menu = TaskActionsMenu(
            items = items(),
        )

        screenshot {
            menu.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }

    @Test
    fun `TaskActionMenuItem screenshot`() {
        val items = items()

        screenshot {
            Column(modifier = Modifier.padding(Spacings.medium)) {
                items.forEach { it.Content() }
            }
        }
    }

    private fun items() = listOf(
        TaskActionMenuItem(
            icon = ImageResource(image = Icons.Default.MonitorHeart),
            title = StringResource("Electrocardiogram"),
            onClick = {},
        ),
        TaskActionMenuItem(
            icon = ImageResource(image = Icons.AutoMirrored.Filled.DirectionsWalk),
            title = StringResource("6-Minute Walk Test"),
            onClick = {},
        ),
        TaskActionMenuItem(
            icon = ImageResource(image = Icons.AutoMirrored.Filled.DirectionsRun),
            title = StringResource("12-Minute Run Test"),
            onClick = {},
        ),
    )
}
