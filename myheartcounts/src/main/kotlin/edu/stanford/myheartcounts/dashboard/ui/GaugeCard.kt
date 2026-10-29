//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A card centering a single [Gauge].
 *
 * @param gauge the gauge in the card, drawn at its own size
 */
@Immutable
data class GaugeCard(
    val gauge: Gauge,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        GroveCard(
            modifier = modifier,
            shape = GroveShapes.large,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacings.medium),
                contentAlignment = Alignment.Center,
            ) {
                gauge.Content()
            }
        }
    }
}

private class GaugeCardPreviewProvider : PreviewParameterProvider<GaugeCard> {

    private val base = GaugeCard(
        gauge = Gauge(
            progress = 0.7f,
            colors = DashboardColorPalette.redToGreen,
            lineWidth = Gauge.defaultLineWidth * 2,
            size = Gauge.Size.Large,
            label = Gauge.Label(
                value = StringResource("70"),
                caption = Gauge.Caption.Range(
                    minimum = StringResource("0"),
                    maximum = StringResource("100"),
                ),
            ),
        ),
    )

    override val values: Sequence<GaugeCard> = sequenceOf(
        base,
        base.copy(
            gauge = base.gauge.copy(
                progress = null,
                colors = listOf(ColorToken.Outline),
                label = base.gauge.label?.copy(value = StringResource("—")),
            ),
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(GaugeCardPreviewProvider::class) content: GaugeCard,
) {
    GroveTheme {
        content.Content(modifier = Modifier.padding(Spacings.medium))
    }
}
