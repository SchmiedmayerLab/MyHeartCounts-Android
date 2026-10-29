//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.Spacings
import org.junit.Test

class ProgressRingRowScreenshotTest : ScreenshotTest() {

    private val base = ProgressRingRow(
        ring = ProgressRing(
            progress = 0.7f,
            progressColor = DashboardColorPalette.green,
            icon = ImageResource.Vector(
                image = Icons.AutoMirrored.Filled.DirectionsWalk,
                tint = { Colors.onSurface },
            ),
        ),
        title = StringResource("Title"),
        subtitle = StringResource("A subtitle describing the goal"),
        trailingBadge = null,
    )

    private val badge = TextBadge(
        text = StringResource("70%"),
        color = DashboardColorPalette.green,
    )

    @Test
    fun `ProgressRingRow screenshot`() {
        screenshot(row = base)
    }

    @Test
    fun `ProgressRingRow without subtitle screenshot`() {
        screenshot(row = base.copy(subtitle = null))
    }

    @Test
    fun `ProgressRingRow without progress screenshot`() {
        screenshot(row = base.copy(ring = base.ring.copy(progress = 0f)))
    }

    @Test
    fun `ProgressRingRow without icon screenshot`() {
        screenshot(row = base.copy(ring = base.ring.copy(icon = null)))
    }

    @Test
    fun `ProgressRingRow long subtitle screenshot`() {
        screenshot(row = base.copy(subtitle = StringResource("A subtitle long enough to wrap onto a second line of the row")))
    }

    @Test
    fun `ProgressRingRow with trailing badge screenshot`() {
        screenshot(row = base.copy(trailingBadge = badge))
    }

    @Test
    fun `ProgressRingRow with trailing badge and long subtitle screenshot`() {
        screenshot(
            row = base.copy(
                subtitle = StringResource("A subtitle long enough to wrap onto a second line of the row"),
                trailingBadge = badge,
            ),
        )
    }

    private fun screenshot(row: ProgressRingRow) {
        screenshot {
            row.Content(
                modifier = Modifier
                    .padding(Spacings.medium)
                    .width(ROW_WIDTH),
            )
        }
    }

    private companion object {
        val ROW_WIDTH = 360.dp
    }
}
