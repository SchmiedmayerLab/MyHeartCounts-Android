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
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A card stacking [ProgressRingRow]s, set apart by dividers that start where the titles start.
 *
 * @param rows the rows, from top to bottom
 */
@Immutable
data class ProgressRingCard(
    val rows: List<ProgressRingRow>,
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
                            start = Spacings.medium + Sizes.Icon.medium + Spacings.medium,
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

private class ProgressRingCardPreviewProvider : PreviewParameterProvider<ProgressRingCard> {

    private val row = ProgressRingRow(
        ring = ProgressRing(
            progress = 1f,
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

    private val base = ProgressRingCard(
        rows = listOf(
            row,
            row.copy(
                ring = row.ring.copy(progress = 0.45f),
                title = StringResource("Another title"),
                trailingBadge = TextBadge(
                    text = StringResource("45%"),
                    color = DashboardColorPalette.green,
                ),
            ),
            row.copy(
                ring = row.ring.copy(
                    progress = 0f,
                    icon = null,
                ),
                title = StringResource("A third title"),
                subtitle = null,
            ),
        ),
    )

    override val values: Sequence<ProgressRingCard> = sequenceOf(
        base,
        base.copy(rows = base.rows.take(1)),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(ProgressRingCardPreviewProvider::class) content: ProgressRingCard,
) {
    GroveTheme {
        content.Content(modifier = Modifier.padding(Spacings.medium))
    }
}
