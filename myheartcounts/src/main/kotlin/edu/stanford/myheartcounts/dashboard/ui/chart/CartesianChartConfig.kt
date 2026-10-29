//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.chart

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.SizeToken
import org.grovealliance.ui.theme.SpacingToken
import org.grovealliance.ui.theme.TextStyleToken

/**
 * Theme-aware appearance of a [CartesianChart], resolved only when rendered.
 *
 * @param backgroundColor the background of the whole chart, including its labels
 * @param plotBackgroundColor the background inside the plot, over [backgroundColor]
 * @param gridColor the grid, baseline, tick and plot border color
 * @param showBorder whether a border frames the plot
 * @param axisTextColor the color of both axes' labels
 * @param emptyTextColor the empty-state text color
 * @param typography the text styles of labels and highlights
 * @param drawing the widths, sizes and spacing of chart marks
 * @param highlight the appearance of the selected-point line and box
 * @param animation how the series are drawn in when the chart appears or its horizontal range
 * changes
 */
@Immutable
data class CartesianChartConfig(
    val backgroundColor: ColorToken = ColorToken.Transparent,
    val plotBackgroundColor: ColorToken = ColorToken.Transparent,
    val gridColor: ColorToken = ColorToken.OutlineVariant,
    val showBorder: Boolean = true,
    val axisTextColor: ColorToken = ColorToken.OnSurfaceVariant,
    val emptyTextColor: ColorToken = ColorToken.OnSurfaceVariant,
    val typography: Typography = Typography(),
    val drawing: Drawing = Drawing(),
    val highlight: Highlight = Highlight(),
    val animation: Animation = Animation(),
) {
    /**
     * The typography of a chart's text, without resolving the current theme.
     *
     * @param axis the style of both axes' labels
     * @param empty the empty-state text style
     * @param highlightTitle the optional heading's style in a highlight box
     * @param highlightValue the style of each value in a highlight box
     * @param highlightValueWeight the weight of highlighted values
     */
    @Immutable
    data class Typography(
        val axis: TextStyleToken = TextStyleToken.LabelSmall,
        val empty: TextStyleToken = TextStyleToken.BodyMedium,
        val highlightTitle: TextStyleToken = TextStyleToken.LabelSmall,
        val highlightValue: TextStyleToken = TextStyleToken.TitleSmall,
        val highlightValueWeight: FontWeight = FontWeight.SemiBold,
    )

    /**
     * The dimensions of axes and series marks.
     *
     * @param gridWidth the width of grid lines, ticks, the baseline and the plot border
     * @param lineWidth the width of lines joining points and the highlight line
     * @param labelSpacing the gap between labels and the plot, also the tick length
     * @param pointRadius the radius of line and point marks; zero hides the dots
     * @param minimumBarWidth the minimum width of a bar with little or no X span
     * @param barWidthFraction the fraction of a point's span occupied by its bar, within zero to one
     * @param xGridDashLength the length of alternating dashes and gaps; null draws solid X grid lines
     */
    @Immutable
    data class Drawing(
        val gridWidth: SizeToken = SizeToken.Border.Small,
        val lineWidth: SizeToken = SizeToken.Border.Medium,
        val labelSpacing: SpacingToken = SpacingToken.ExtraSmall,
        val pointRadius: Dp = 2.dp,
        val minimumBarWidth: Dp = 2.dp,
        val barWidthFraction: Float = DEFAULT_BAR_WIDTH_FRACTION,
        val xGridDashLength: Dp? = 2.dp,
    ) {
        init {
            gridWidth.requirePositive(name = "gridWidth")
            lineWidth.requirePositive(name = "lineWidth")
            labelSpacing.requireNonnegative(name = "labelSpacing")
            require(pointRadius.value.isFinite() && pointRadius >= 0.dp) { "pointRadius must be finite and nonnegative" }
            require(minimumBarWidth.value.isFinite() && minimumBarWidth > 0.dp) { "minimumBarWidth must be finite and positive" }
            require(barWidthFraction.isFinite() && barWidthFraction > 0f && barWidthFraction <= 1f) {
                "barWidthFraction must be in (0, 1]"
            }
            require(xGridDashLength == null || (xGridDashLength.value.isFinite() && xGridDashLength > 0.dp)) {
                "xGridDashLength must be null or finite and positive"
            }
        }

        private companion object {
            /**
             * The share of each point's X interval occupied by its bar.
             */
            const val DEFAULT_BAR_WIDTH_FRACTION = 0.4f
        }
    }

    /**
     * How the series are drawn in: lines are revealed from left to right, while bars grow from zero
     * and dots grow from nothing, each starting later the further right it is. Axis labels and grid
     * lines fade in over the first part of the animation. Points cannot be highlighted until the
     * animation ends.
     *
     * @param enabled whether the series are drawn in; when not, the chart is drawn complete at once
     * @param durationMillis the length of the animation
     * @param stagger the fraction of [durationMillis] by which the rightmost mark starts after the
     * leftmost, within zero to below one
     */
    @Immutable
    data class Animation(
        val enabled: Boolean = true,
        val durationMillis: Int = DEFAULT_DURATION_MILLIS,
        val stagger: Float = DEFAULT_STAGGER,
    ) {
        init {
            require(durationMillis > 0) { "durationMillis must be positive" }
            require(stagger.isFinite() && stagger >= 0f && stagger < 1f) { "stagger must be in [0, 1)" }
        }

        private companion object {
            /**
             * The length of the animation drawing the series in.
             */
            const val DEFAULT_DURATION_MILLIS = 700

            /**
             * The fraction of the animation by which the rightmost mark starts after the leftmost.
             */
            const val DEFAULT_STAGGER = 0.5f
        }
    }

    /**
     * Colors and dimensions of the selected-point line and box.
     *
     * @param backgroundColor the box's background
     * @param lineColor the vertical selection line color, before [lineAlpha]
     * @param titleColor the optional heading's color
     * @param valueColor the highlighted values' text color
     * @param lineAlpha the selection line opacity multiplier, within zero to one
     * @param shape the highlight box shape
     * @param padding the box's inset and the gap after its series dots
     * @param dotRadius the radius of each series' color dot
     */
    @Immutable
    data class Highlight(
        val backgroundColor: ColorToken = ColorToken.SurfaceContainerHighest,
        val lineColor: ColorToken = ColorToken.Outline,
        val titleColor: ColorToken = ColorToken.OnSurfaceVariant,
        val valueColor: ColorToken = ColorToken.OnSurface,
        val lineAlpha: Float = DEFAULT_LINE_ALPHA,
        val shape: Shape = GroveShapes.small,
        val padding: SpacingToken = SpacingToken.ExtraSmall,
        val dotRadius: Dp = 3.dp,
    ) {
        init {
            padding.requireNonnegative(name = "padding")
            require(lineAlpha.isFinite() && lineAlpha in 0f..1f) { "lineAlpha must be within zero to one" }
            require(dotRadius.value.isFinite() && dotRadius >= 0.dp) { "dotRadius must be finite and nonnegative" }
        }

        private companion object {
            /**
             * The opacity multiplier of the line marking the selected X coordinate.
             */
            const val DEFAULT_LINE_ALPHA = 0.3f
        }
    }
}

private fun SizeToken.requirePositive(name: String) {
    if (this is SizeToken.Custom) {
        require(size.value.isFinite() && size > 0.dp) { "$name must be finite and positive" }
    }
}

private fun SpacingToken.requireNonnegative(name: String) {
    if (this is SpacingToken.Custom) {
        require(spacing.value.isFinite() && spacing >= 0.dp) { "$name must be finite and nonnegative" }
    }
}
