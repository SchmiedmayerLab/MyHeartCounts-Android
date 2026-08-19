//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.scheduler

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.ui.Modifier
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Spacings
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
