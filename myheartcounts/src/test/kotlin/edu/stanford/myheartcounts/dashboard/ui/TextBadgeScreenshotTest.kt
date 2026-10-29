//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.Spacings
import org.junit.Test

class TextBadgeScreenshotTest : ScreenshotTest() {

    private val base = TextBadge(
        text = StringResource("45%"),
        color = DashboardColorPalette.green,
    )

    @Test
    fun `TextBadge screenshot`() {
        screenshot(badge = base)
    }

    @Test
    fun `TextBadge theme color screenshot`() {
        screenshot(badge = base.copy(color = ColorToken.Primary))
    }

    @Test
    fun `TextBadge long text screenshot`() {
        screenshot(badge = base.copy(text = StringResource("A longer badge")))
    }

    private fun screenshot(badge: TextBadge) {
        screenshot {
            badge.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }
}
