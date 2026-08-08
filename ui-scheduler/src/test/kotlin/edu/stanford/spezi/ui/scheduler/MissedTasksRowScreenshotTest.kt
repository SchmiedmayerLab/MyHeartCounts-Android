//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui.scheduler

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.ui.Modifier
import edu.stanford.spezi.testing.screenshot.ScreenshotTest
import edu.stanford.spezi.ui.ImageResource
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Spacings
import org.junit.Test

class MissedTasksRowScreenshotTest : ScreenshotTest() {

    @Test
    fun `MissedTasksRow screenshot`() {
        val row = MissedTasksRow(
            icon = ImageResource(image = Icons.Default.CalendarMonth),
            title = StringResource("Missed Tasks"),
            subtitle = StringResource("3 missed tasks in the past 2 weeks"),
            onClick = {},
        )

        screenshot {
            row.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }
}
