//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.Spacings
import org.junit.Test

class GaugeTileScreenshotTest : ScreenshotTest() {

    private val base = GaugeTile(
        title = StringResource("Title"),
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

    @Test
    fun `GaugeTile screenshot`() {
        screenshot(tile = base)
    }

    @Test
    fun `GaugeTile without value screenshot`() {
        screenshot(
            tile = base.copy(
                gauge = base.gauge.copy(
                    progress = null,
                    colors = listOf(ColorToken.Outline),
                    label = base.gauge.label?.copy(
                        value = StringResource("—"),
                        caption = Gauge.Caption.Description(text = StringResource("Tap to learn more…")),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `GaugeTile without caption screenshot`() {
        screenshot(tile = base.copy(gauge = base.gauge.copy(label = base.gauge.label?.copy(caption = null))))
    }

    @Test
    fun `GaugeTile long title screenshot`() {
        screenshot(tile = base.copy(title = StringResource("A title long enough to wrap")))
    }

    @Test
    fun `GaugeTile title too long for two lines screenshot`() {
        screenshot(
            tile = base.copy(
                title = StringResource("A title so long that it does not fit onto two lines and has to be cut off"),
            ),
        )
    }

    @Test
    fun `GaugeTile not clickable screenshot`() {
        screenshot(tile = base.copy(onClick = null))
    }

    @Test
    fun `GaugeTile wide screenshot`() {
        screenshot(
            tile = base,
            width = WIDE_TILE_WIDTH,
        )
    }

    private fun screenshot(
        tile: GaugeTile,
        width: Dp = TILE_WIDTH,
    ) {
        screenshot {
            tile.Content(
                modifier = Modifier
                    .padding(Spacings.medium)
                    .width(width),
            )
        }
    }

    private companion object {
        val TILE_WIDTH = 180.dp
        val WIDE_TILE_WIDTH = 360.dp
    }
}
