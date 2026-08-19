//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.scheduler

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Spacings
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
