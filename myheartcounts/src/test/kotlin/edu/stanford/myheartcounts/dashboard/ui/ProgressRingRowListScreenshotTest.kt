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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.Spacings
import org.junit.Test

class ProgressRingRowListScreenshotTest : ScreenshotTest() {

    private val base = ProgressRingRowList(
        label = StringResource("Label"),
        rows = listOf(
            ProgressRingRow(
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
            ),
            ProgressRingRow(
                ring = ProgressRing(
                    progress = 0f,
                    progressColor = DashboardColorPalette.green,
                    icon = ImageResource.Vector(
                        image = Icons.AutoMirrored.Filled.Assignment,
                        tint = { Colors.onSurface },
                    ),
                ),
                title = StringResource("Another title"),
                subtitle = StringResource("Another subtitle"),
                trailingBadge = null,
            ),
            ProgressRingRow(
                ring = ProgressRing(
                    progress = 0f,
                    progressColor = DashboardColorPalette.green,
                    icon = null,
                ),
                title = StringResource("A third title"),
                subtitle = null,
                trailingBadge = null,
            ),
        ),
    )

    @Test
    fun `ProgressRingRowList screenshot`() {
        screenshot(list = base)
    }

    @Test
    fun `ProgressRingRowList without label screenshot`() {
        screenshot(list = base.copy(label = null))
    }

    @Test
    fun `ProgressRingRowList single row screenshot`() {
        screenshot(list = base.copy(rows = base.rows.take(1)))
    }

    private fun screenshot(list: ProgressRingRowList) {
        screenshot {
            list.Content(
                modifier = Modifier
                    .padding(Spacings.medium)
                    .width(LIST_WIDTH),
            )
        }
    }

    private companion object {
        val LIST_WIDTH = 360.dp
    }
}
