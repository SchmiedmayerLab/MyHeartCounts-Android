//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.Spacings
import org.junit.Test

class GaugeTileGridScreenshotTest : ScreenshotTest() {

    private val tile = GaugeTile(
        title = StringResource("Sleep"),
        gauge = Gauge(
            progress = 1f,
            colors = DashboardColorPalette.redToGreen,
            lineWidth = Gauge.defaultLineWidth * 1.5f,
            size = Gauge.Size.Regular,
            label = Gauge.Label(
                value = StringResource("100"),
                caption = Gauge.Caption.Description(text = StringResource("7:36")),
            ),
        ),
        onClick = {},
    )

    private val base = GaugeTileGrid(
        tiles = listOf(
            tile.copy(title = StringResource("Exercise Minutes")),
            tile,
            tile.copy(
                title = StringResource("Diet"),
                gauge = tile.gauge.copy(
                    progress = null,
                    colors = listOf(ColorToken.Outline),
                    label = tile.gauge.label?.copy(
                        value = StringResource("—"),
                        caption = Gauge.Caption.Description(text = StringResource("Tap to learn more…")),
                    ),
                ),
            ),
            tile.copy(title = StringResource("Mental Well Being")),
        ),
    )

    @Test
    fun `GaugeTileGrid screenshot`() {
        screenshot(grid = base)
    }

    @Test
    fun `GaugeTileGrid odd number of tiles screenshot`() {
        screenshot(grid = base.copy(tiles = base.tiles.dropLast(1)))
    }

    @Test
    fun `GaugeTileGrid tiles without caption beside tiles with one screenshot`() {
        screenshot(
            grid = base.copy(
                tiles = base.tiles.mapIndexed { index, tile ->
                    if (index % 2 == 0) {
                        tile.copy(gauge = tile.gauge.copy(label = tile.gauge.label?.copy(caption = null)))
                    } else {
                        tile
                    }
                },
            ),
        )
    }

    private fun screenshot(grid: GaugeTileGrid) {
        screenshot {
            grid.Content(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacings.medium),
            )
        }
    }
}
