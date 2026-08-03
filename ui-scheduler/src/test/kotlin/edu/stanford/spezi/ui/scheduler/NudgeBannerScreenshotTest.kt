//
// This source file is part of the My Heart Counts open-source project
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

class NudgeBannerScreenshotTest : ScreenshotTest() {

    @Test
    fun `NudgeBanner screenshot`() {
        val banner = NudgeBanner(
            title = StringResource("Keep it up!"),
            message = StringResource(
                "You have walked more this week than last week. A short walk today keeps the streak going."
            ),
        )

        screenshot {
            banner.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }
}
