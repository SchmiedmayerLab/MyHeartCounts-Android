//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.ui.Modifier
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.junit.Test

class ProgressRingScreenshotTest : ScreenshotTest() {

    private val base = ProgressRing(
        progress = 0.7f,
        progressColor = DashboardColorPalette.green,
        icon = ImageResource.Vector(
            image = Icons.AutoMirrored.Filled.DirectionsWalk,
            tint = { Colors.onSurface },
        ),
    )

    @Test
    fun `ProgressRing screenshot`() {
        screenshot(ring = base)
    }

    @Test
    fun `ProgressRing empty screenshot`() {
        screenshot(ring = base.copy(progress = 0f))
    }

    @Test
    fun `ProgressRing full screenshot`() {
        screenshot(ring = base.copy(progress = 1f))
    }

    @Test
    fun `ProgressRing without icon screenshot`() {
        screenshot(ring = base.copy(icon = null))
    }

    @Test
    fun `ProgressRing other color screenshot`() {
        screenshot(ring = base.copy(progressColor = DashboardColorPalette.blue))
    }

    private fun screenshot(ring: ProgressRing) {
        screenshot {
            ring.Content(
                modifier = Modifier
                    .padding(Spacings.medium)
                    .size(Sizes.Icon.medium),
            )
        }
    }
}
