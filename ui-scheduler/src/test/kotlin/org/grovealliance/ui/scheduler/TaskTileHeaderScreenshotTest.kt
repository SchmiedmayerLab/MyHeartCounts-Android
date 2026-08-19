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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Spacings
import org.junit.Test

class TaskTileHeaderScreenshotTest : ScreenshotTest() {

    @Test
    fun `TaskTileHeader leading screenshot`() {
        val header = TaskTileHeader(
            icon = ImageResource(image = Icons.AutoMirrored.Filled.Assignment),
            title = StringResource("Daily Check-In"),
            categoryLabel = StringResource("Questionnaire"),
            timing = StringResource("9:00 AM"),
        )

        screenshot {
            header.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }

    @Test
    fun `TaskTileHeader title only screenshot`() {
        val header = TaskTileHeader(
            icon = ImageResource(image = Icons.AutoMirrored.Filled.Assignment),
            title = StringResource("Daily Check-In"),
        )

        screenshot {
            header.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }

    @Test
    fun `TaskTileHeader without icon screenshot`() {
        val header = TaskTileHeader(
            icon = null,
            title = StringResource("Daily Check-In"),
            categoryLabel = StringResource("Questionnaire"),
            timing = StringResource("9:00 AM"),
        )

        screenshot {
            header.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }

    @Test
    fun `TaskTileHeader centered screenshot`() {
        val header = TaskTileHeader(
            icon = ImageResource(image = Icons.AutoMirrored.Filled.Assignment),
            title = StringResource("Daily Check-In"),
            categoryLabel = StringResource("Questionnaire"),
            timing = StringResource("9:00 AM"),
            horizontalAlignment = Alignment.CenterHorizontally,
        )

        screenshot {
            header.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }
}
