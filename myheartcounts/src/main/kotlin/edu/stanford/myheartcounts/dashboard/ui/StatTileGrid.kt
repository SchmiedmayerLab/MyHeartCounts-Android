//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import edu.stanford.myheartcounts.dashboard.ui.internal.TileGrid
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.resolve

/**
 * A grid of [StatTile]s, two side by side in rows of equal height.
 *
 * @param tiles the tiles, in reading order
 */
@Immutable
data class StatTileGrid(
    val tiles: List<StatTile>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        TileGrid(
            tiles = tiles,
            modifier = modifier,
        )
    }
}

private class StatTileGridPreviewProvider : PreviewParameterProvider<StatTileGrid> {

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

    override val values: Sequence<StatTileGrid> = sequenceOf(
        base,
        base.copy(tiles = base.tiles.dropLast(1)),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(StatTileGridPreviewProvider::class) content: StatTileGrid,
) {
    GroveTheme {
        content.Content(modifier = Modifier.padding(Spacings.medium))
    }
}
