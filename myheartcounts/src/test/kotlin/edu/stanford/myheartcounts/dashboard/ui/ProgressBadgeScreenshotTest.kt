//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.ui.Modifier
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.Spacings
import org.junit.Test

class ProgressBadgeScreenshotTest : ScreenshotTest() {

    private val base = ProgressBadge(
        icon = ImageResource.Vector(
            image = Icons.Default.MilitaryTech,
            tint = { Colors.onSurface },
        ),
        label = StringResource("5 / 20"),
        progress = 0.25f,
        progressColor = ColorToken.Primary,
    )

    @Test
    fun `ProgressBadge screenshot`() {
        screenshot(badge = base)
    }

    @Test
    fun `ProgressBadge without icon screenshot`() {
        screenshot(badge = base.copy(icon = null))
    }

    @Test
    fun `ProgressBadge empty screenshot`() {
        screenshot(badge = base.copy(progress = 0f))
    }

    @Test
    fun `ProgressBadge full screenshot`() {
        screenshot(badge = base.copy(progress = 1f))
    }

    @Test
    fun `ProgressBadge other color screenshot`() {
        screenshot(badge = base.copy(progressColor = DashboardColorPalette.green))
    }

    private fun screenshot(badge: ProgressBadge) {
        screenshot {
            badge.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }
}
