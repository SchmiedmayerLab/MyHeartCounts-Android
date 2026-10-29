//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.Modifier
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.resolve
import org.junit.Test

class StatTileGridScreenshotTest : ScreenshotTest() {

    private val base = StatTileGrid(
        tiles = listOf(
            StatTile(
                icon = ImageResource.Vector(
                    image = Icons.AutoMirrored.Filled.DirectionsWalk,
                    tint = { DashboardColorPalette.green.resolve() },
                ),
                title = StringResource("Best Step Day"),
                value = StatTile.Value(
                    text = StringResource("8K"),
                    unit = null,
                ),
                footnote = StringResource("24. Oct"),
            ),
            StatTile(
                icon = ImageResource.Vector(
                    image = Icons.Default.Timer,
                    tint = { DashboardColorPalette.blue.resolve() },
                ),
                title = StringResource("Longest Workout"),
                value = StatTile.Value(
                    text = StringResource("1h 34m"),
                    unit = null,
                ),
                footnote = StringResource("20. Oct • Walking"),
            ),
            StatTile(
                icon = ImageResource.Vector(
                    image = Icons.Default.Favorite,
                    tint = { DashboardColorPalette.red.resolve() },
                ),
                title = StringResource("Max Heart Rate"),
                value = StatTile.Value(
                    text = StringResource("169"),
                    unit = StringResource("bpm"),
                ),
                footnote = null,
            ),
            StatTile(
                icon = ImageResource.Vector(
                    image = Icons.Default.Hotel,
                    tint = { DashboardColorPalette.pink.resolve() },
                ),
                title = StringResource("Resting HR"),
                value = StatTile.Value(
                    text = StringResource("80"),
                    unit = StringResource("bpm"),
                ),
                footnote = StringResource("Average"),
            ),
        ),
    )

    @Test
    fun `StatTileGrid screenshot`() {
        screenshot(grid = base)
    }

    @Test
    fun `StatTileGrid odd number of tiles screenshot`() {
        screenshot(grid = base.copy(tiles = base.tiles.dropLast(1)))
    }

    @Test
    fun `StatTileGrid single tile screenshot`() {
        screenshot(grid = base.copy(tiles = base.tiles.take(1)))
    }

    @Test
    fun `StatTileGrid without values screenshot`() {
        screenshot(grid = base.copy(tiles = base.tiles.map { it.copy(value = null) }))
    }

    private fun screenshot(grid: StatTileGrid) {
        screenshot {
            grid.Content(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacings.medium),
            )
        }
    }
}
