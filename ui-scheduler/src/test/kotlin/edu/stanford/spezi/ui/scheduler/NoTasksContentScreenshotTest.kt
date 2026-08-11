//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui.scheduler

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.ui.Modifier
import edu.stanford.spezi.testing.screenshot.ScreenshotTest
import edu.stanford.spezi.ui.ImageResource
import edu.stanford.spezi.ui.StringResource
import org.junit.Test

class NoTasksContentScreenshotTest : ScreenshotTest() {

    @Test
    fun `NoTasksContent with description screenshot`() {
        val content = NoTasksContent(
            icon = ImageResource(image = Icons.Default.Celebration),
            title = StringResource("You're All Set"),
            description = StringResource("All tasks have been completed!"),
        )

        screenshot {
            content.Content(modifier = Modifier.fillMaxWidth())
        }
    }

    @Test
    fun `NoTasksContent title only screenshot`() {
        val content = NoTasksContent(
            icon = ImageResource(image = Icons.Default.Celebration),
            title = StringResource("No Upcoming Tasks"),
        )

        screenshot {
            content.Content(modifier = Modifier.fillMaxWidth())
        }
    }
}
