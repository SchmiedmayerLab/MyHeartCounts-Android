//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import edu.stanford.myheartcounts.dashboard.ui.internal.TileHeader
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A titled tile showing a [Gauge].
 *
 * The title sits above a divider; the gauge is centered beneath it.
 *
 * @param title the heading of the tile
 * @param gauge the gauge shown below the title
 * @param onClick invoked when the tile is tapped; the tile is not tappable when omitted
 */
@Immutable
data class GaugeTile(
    val title: StringResource,
    val gauge: Gauge,
    val onClick: (() -> Unit)?,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val shape = GroveShapes.large
        val clickModifier = onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier
        GroveCard(
            modifier = modifier
                .clip(shape)
                .then(clickModifier),
            shape = shape,
        ) {
            Column(
                modifier = Modifier.padding(
                    start = Spacings.small,
                    end = Spacings.small,
                    bottom = Spacings.small,
                ),
            ) {
                TileHeader(
                    title = title,
                    trailing = null,
                )
                HorizontalDivider()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacings.small),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    gauge.Content()
                }
            }
        }
    }
}

private class GaugeTilePreviewProvider : PreviewParameterProvider<GaugeTile> {

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

    override val values: Sequence<GaugeTile> = sequenceOf(
        base,
        base.copy(
            gauge = base.gauge.copy(
                progress = null,
                colors = listOf(ColorToken.Outline),
                label = base.gauge.label?.copy(
                    value = StringResource("—"),
                    caption = Gauge.Caption.Description(text = StringResource("Tap to learn more…")),
                ),
            ),
        ),
        base.copy(title = StringResource("A title long enough to wrap onto a second line")),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(GaugeTilePreviewProvider::class) content: GaugeTile,
) {
    GroveTheme {
        content.Content(
            modifier = Modifier
                .padding(Spacings.medium)
                .width(width = 180.dp),
        )
    }
}
