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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.resolve

/**
 * A card stacking [IconTextCardRow]s, set apart by dividers that start where the texts start.
 *
 * @param rows the rows, from top to bottom
 */
@Immutable
data class IconTextCard(
    val rows: List<IconTextCardRow>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        GroveCard(
            modifier = modifier,
            shape = GroveShapes.large,
        ) {
            rows.forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(
                            start = Spacings.medium + Sizes.Icon.small + Spacings.medium,
                            end = Spacings.medium,
                        ),
                    )
                }
                row.Content(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacings.medium),
                )
            }
        }
    }
}

private class IconTextCardPreviewProvider : PreviewParameterProvider<IconTextCard> {

    private val base = IconTextCard(
        rows = listOf(
            IconTextCardRow(
                icon = ImageResource.Vector(
                    image = Icons.AutoMirrored.Filled.DirectionsWalk,
                    tint = { DashboardColorPalette.green.resolve() },
                ),
                text = StringResource("Your 26,635 steps cover about 26 km — that's 9.6 lengths of a long bridge."),
            ),
            IconTextCardRow(
                icon = ImageResource.Vector(
                    image = Icons.Default.Favorite,
                    tint = { DashboardColorPalette.red.resolve() },
                ),
                text = StringResource("Your heart has beaten about 681K times since you started."),
            ),
            IconTextCardRow(
                icon = ImageResource.Vector(
                    image = Icons.Default.LocalFireDepartment,
                    tint = { DashboardColorPalette.orange.resolve() },
                ),
                text = StringResource("You've burned 2,053 active calories — the equivalent of 7 slices of pizza."),
            ),
            IconTextCardRow(
                icon = ImageResource.Vector(
                    image = Icons.Default.Hotel,
                    tint = { DashboardColorPalette.indigo.resolve() },
                ),
                text = StringResource("A short text"),
            ),
        ),
    )

    override val values: Sequence<IconTextCard> = sequenceOf(
        base,
        base.copy(rows = base.rows.take(1)),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(IconTextCardPreviewProvider::class) content: IconTextCard,
) {
    GroveTheme {
        content.Content(modifier = Modifier.padding(Spacings.medium))
    }
}
