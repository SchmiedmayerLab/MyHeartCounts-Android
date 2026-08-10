//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui.scheduler

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import edu.stanford.spezi.testing.screenshot.ScreenshotTest
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Spacings
import org.junit.Test

class TaskListHeaderScreenshotTest : ScreenshotTest() {

    @Test
    fun `TaskListHeader with subtitle screenshot`() {
        val header = TaskListHeader(
            title = StringResource("Next 2 Weeks"),
            subtitle = StringResource("01/08/2026 – 15/08/2026"),
        )

        screenshot {
            header.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }

    @Test
    fun `TaskListHeader title only screenshot`() {
        val header = TaskListHeader(
            title = StringResource("Today's Tasks"),
        )

        screenshot {
            header.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }
}
