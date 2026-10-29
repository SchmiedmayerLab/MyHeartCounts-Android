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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.resolve
import org.junit.Test

class StatTileScreenshotTest : ScreenshotTest() {

    private val base = StatTile(
        icon = ImageResource.Vector(
            image = Icons.Default.Favorite,
            tint = { DashboardColorPalette.pink.resolve() },
        ),
        title = StringResource("Resting HR"),
        value = StatTile.Value(
            text = StringResource("80"),
            unit = StringResource("bpm"),
        ),
        footnote = StringResource("Average"),
    )

    @Test
    fun `StatTile screenshot`() {
        screenshot(tile = base)
    }

    @Test
    fun `StatTile without unit screenshot`() {
        screenshot(tile = base.copy(value = base.value?.copy(unit = null)))
    }

    @Test
    fun `StatTile without footnote screenshot`() {
        screenshot(tile = base.copy(footnote = null))
    }

    @Test
    fun `StatTile without value screenshot`() {
        screenshot(tile = base.copy(value = null))
    }

    @Test
    fun `StatTile without value or footnote screenshot`() {
        screenshot(
            tile = base.copy(
                value = null,
                footnote = null,
            ),
        )
    }

    @Test
    fun `StatTile long title screenshot`() {
        screenshot(tile = base.copy(title = StringResource("A title too long for a single line")))
    }

    @Test
    fun `StatTile long value screenshot`() {
        screenshot(tile = base.copy(value = base.value?.copy(text = StringResource("1,234,567,890"))))
    }

    @Test
    fun `StatTile long footnote screenshot`() {
        screenshot(tile = base.copy(footnote = StringResource("A footnote too long for a single line")))
    }

    private fun screenshot(tile: StatTile) {
        screenshot {
            tile.Content(
                modifier = Modifier
                    .padding(Spacings.medium)
                    .width(TILE_WIDTH),
            )
        }
    }

    private companion object {
        val TILE_WIDTH = 180.dp
    }
}
