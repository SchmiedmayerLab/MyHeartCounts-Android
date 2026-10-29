//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.medium
import org.grovealliance.ui.theme.resolve
import org.grovealliance.ui.theme.semiBold

/**
 * A stack of colored bands, each labeled at its start and its end, one of which may be
 * highlighted.
 *
 * The bands are drawn top to bottom in the given order, set slightly apart, with the outer corners
 * of the stack rounded. The labels are drawn in [DashboardColorPalette.onRating], so band colors
 * must be light enough to carry dark text in both themes.
 *
 * @param bands the bands, from top to bottom
 */
@Immutable
data class ColorBandList(
    val bands: List<Band>,
) : ComposableContent {

    /**
     * A single band of a [ColorBandList].
     *
     * @param leadingText the label at the start of the band; omitted when there is none
     * @param trailingText the label at the end of the band; omitted when there is none
     * @param fillColors the colors filling the band, spread evenly from its start to its end; a
     * single color fills the band uniformly. Must not be empty.
     * @param isHighlighted whether the band stands out from the others, with its labels drawn in a
     * heavier weight
     */
    data class Band(
        val leadingText: StringResource?,
        val trailingText: StringResource?,
        val fillColors: List<ColorToken>,
        val isHighlighted: Boolean,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier.clip(GroveShapes.large),
            verticalArrangement = Arrangement.spacedBy(Spacings.extraSmall),
        ) {
            bands.forEachIndexed { index, band ->
                val colors = band.fillColors.map { it.resolve() }
                val fill = colors.singleOrNull()?.let { SolidColor(value = it) } ?: Brush.horizontalGradient(colors = colors)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GroveShapes.medium)
                        .background(brush = fill)
                        .semantics(mergeDescendants = true) { selected = band.isHighlighted }
                        .padding(
                            start = Spacings.medium,
                            end = Spacings.medium,
                            top = if (index == 0) Spacings.small + Spacings.extraSmall else Spacings.small,
                            bottom = if (index == bands.lastIndex) Spacings.small + Spacings.extraSmall else Spacings.small,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(Spacings.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (band.leadingText != null) {
                        BandText(
                            text = band.leadingText,
                            isHighlighted = band.isHighlighted,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (band.trailingText != null) {
                        BandText(
                            text = band.trailingText,
                            isHighlighted = band.isHighlighted,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun BandText(
        text: StringResource,
        isHighlighted: Boolean,
        modifier: Modifier = Modifier,
    ) {
        Text(
            text = text.text(),
            style = if (isHighlighted) TextStyles.bodyMedium.semiBold() else TextStyles.bodyMedium.medium(),
            color = DashboardColorPalette.onRating.resolve(),
            modifier = modifier,
        )
    }
}

private class ColorBandListPreviewProvider : PreviewParameterProvider<ColorBandList> {

    private val base = ColorBandList(
        bands = listOf(
            ColorBandList.Band(
                leadingText = StringResource("7 to 9 hours"),
                trailingText = StringResource("100"),
                fillColors = listOf(DashboardColorPalette.green),
                isHighlighted = true,
            ),
            ColorBandList.Band(
                leadingText = StringResource("9 to 10 hours"),
                trailingText = StringResource("90"),
                fillColors = listOf(DashboardColorPalette.redToGreen.colorAt(fraction = 0.9f)),
                isHighlighted = false,
            ),
            ColorBandList.Band(
                leadingText = StringResource("6 to 7 hours"),
                trailingText = StringResource("70"),
                fillColors = listOf(DashboardColorPalette.redToGreen.colorAt(fraction = 0.7f)),
                isHighlighted = false,
            ),
            ColorBandList.Band(
                leadingText = StringResource("4 to 5 hours"),
                trailingText = StringResource("20"),
                fillColors = listOf(DashboardColorPalette.redToGreen.colorAt(fraction = 0.2f)),
                isHighlighted = false,
            ),
        ),
    )

    override val values: Sequence<ColorBandList> = sequenceOf(
        base,
        base.copy(bands = base.bands.map { it.copy(isHighlighted = false) }),
        base.copy(bands = base.bands.map { it.copy(trailingText = null) }),
        base.copy(
            bands = listOf(
                base.bands.first().copy(
                    leadingText = StringResource("0"),
                    fillColors = DashboardColorPalette.redToGreen,
                    isHighlighted = false,
                ),
            ),
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(ColorBandListPreviewProvider::class) content: ColorBandList,
) {
    GroveTheme {
        content.Content(
            modifier = Modifier
                .padding(Spacings.medium)
                .width(width = 360.dp),
        )
    }
}
