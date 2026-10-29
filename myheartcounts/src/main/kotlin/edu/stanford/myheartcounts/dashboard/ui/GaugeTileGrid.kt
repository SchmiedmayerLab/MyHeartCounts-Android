//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import edu.stanford.myheartcounts.dashboard.ui.internal.TileGrid
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A grid of [GaugeTile]s, two side by side in rows of equal height.
 *
 * @param tiles the tiles, in reading order
 */
@Immutable
data class GaugeTileGrid(
    val tiles: List<GaugeTile>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        TileGrid(
            tiles = tiles,
            modifier = modifier,
        )
    }
}

private class GaugeTileGridPreviewProvider : PreviewParameterProvider<GaugeTileGrid> {

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

    private val emptyTile = tile.copy(
        title = StringResource("Mental Well Being"),
        gauge = tile.gauge.copy(
            progress = null,
            colors = listOf(ColorToken.Outline),
            label = tile.gauge.label?.copy(
                value = StringResource("—"),
                caption = Gauge.Caption.Description(text = StringResource("Tap to learn more…")),
            ),
        ),
    )

    private val base = GaugeTileGrid(
        tiles = listOf(
            tile.copy(title = StringResource("Exercise Minutes")),
            tile,
            emptyTile.copy(title = StringResource("Diet")),
            emptyTile,
        ),
    )

    override val values: Sequence<GaugeTileGrid> = sequenceOf(
        base,
        base.copy(tiles = base.tiles.dropLast(1)),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(GaugeTileGridPreviewProvider::class) content: GaugeTileGrid,
) {
    GroveTheme {
        content.Content(modifier = Modifier.padding(Spacings.medium))
    }
}
