//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.bold
import org.grovealliance.ui.theme.resolve
import org.grovealliance.ui.theme.semiBold
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.geometry.Size as GeometrySize

/**
 * An open arc spanning two thirds of a circle, with its opening at the bottom, marking a value
 * between the arc's start and end.
 *
 * The whole arc is always drawn, filled with [colors] from its start to its end. The value is
 * marked by a dot on the arc, set off from it by a gap on either side; without a value, no dot is
 * drawn.
 *
 * @param progress the position of the marked value, from `0` at the start of the arc to `1` at its
 * end; must lie within that range. `null` marks no value.
 * @param colors the colors filling the arc, spread evenly from its start to its end; a single color
 * fills the arc uniformly. Must not be empty.
 * @param lineWidth the thickness of the arc
 * @param size how large the gauge and its texts are drawn
 * @param label the texts accompanying the gauge; omitted when there is none
 */
@Immutable
data class Gauge(
    val progress: Float?,
    val colors: List<ColorToken>,
    val lineWidth: Dp,
    val size: Size,
    val label: Label?,
) : ComposableContent {

    init {
        require(progress == null || progress in 0f..1f) {
            "progress must lie within 0 and 1, but was $progress"
        }
    }

    /**
     * How large the gauge and its texts are drawn.
     */
    enum class Size {
        Regular,
        Large,
    }

    /**
     * The texts accompanying a gauge.
     *
     * The value, and a [Caption.Range] with it, are drawn muted when the gauge marks no value.
     *
     * @param value the text at the center of the gauge, already formatted
     * @param caption the text accompanying the value; omitted when there is none
     */
    data class Label(
        val value: StringResource,
        val caption: Caption?,
    )

    /**
     * The text accompanying the value of a gauge.
     */
    sealed interface Caption {
        /**
         * A short text centered below the gauge, reaching up into its open bottom.
         *
         * @param text the text itself
         */
        data class Description(val text: StringResource) : Caption

        /**
         * The labels of the gauge's two ends, below the start and the end of the arc.
         *
         * @param minimum the label below the start of the arc
         * @param maximum the label below the end of the arc
         */
        data class Range(
            val minimum: StringResource,
            val maximum: StringResource,
        ) : Caption
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val arcColors = colors.map { it.resolve() }
        val caption = label?.caption
        val textColor = if (progress == null) Colors.onSurfaceVariant else Colors.onSurface
        val captionStyle = when (size) {
            Size.Regular -> TextStyles.bodySmall
            Size.Large -> TextStyles.bodyLarge
        }
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(-DESCRIPTION_OVERLAP),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(
                    when (size) {
                        Size.Regular -> REGULAR_DIAMETER
                        Size.Large -> LARGE_DIAMETER
                    },
                ),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .semantics {
                            if (progress != null) {
                                progressBarRangeInfo = ProgressBarRangeInfo(
                                    current = progress,
                                    range = 0f..1f,
                                )
                            }
                        },
                ) {
                    val strokeWidth = lineWidth.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    rotate(degrees = START_ANGLE_DEGREES) {
                        drawArc(
                            brush = arcBrush(colors = arcColors),
                            startAngle = 0f,
                            sweepAngle = SWEEP_DEGREES,
                            useCenter = false,
                            topLeft = Offset(
                                x = center.x - radius,
                                y = center.y - radius,
                            ),
                            size = GeometrySize(
                                width = radius * 2,
                                height = radius * 2,
                            ),
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                            ),
                        )
                        if (progress != null) {
                            val angle = Math.toRadians((SWEEP_DEGREES * progress).toDouble())
                            val gapWidth = strokeWidth * GAP_WIDTH_FRACTION
                            drawCircle(
                                color = GAP_COLOR,
                                radius = (strokeWidth + gapWidth) / 2,
                                center = Offset(
                                    x = center.x + radius * cos(angle).toFloat(),
                                    y = center.y + radius * sin(angle).toFloat(),
                                ),
                                style = Stroke(width = gapWidth),
                                blendMode = BlendMode.DstOut,
                            )
                        }
                    }
                }
                if (label != null) {
                    Text(
                        text = label.value.text(),
                        style = when (size) {
                            Size.Regular -> TextStyles.titleSmall.semiBold()
                            Size.Large -> TextStyles.displaySmall.bold()
                        },
                        color = textColor,
                        maxLines = 1,
                    )
                }
                if (caption is Caption.Range) {
                    Text(
                        text = caption.minimum.text(),
                        style = captionStyle,
                        color = textColor,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = Spacings.small),
                    )
                    Text(
                        text = caption.maximum.text(),
                        style = captionStyle,
                        color = textColor,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = Spacings.small),
                    )
                }
            }
            if (caption is Caption.Description) {
                Text(
                    text = caption.text.text(),
                    style = captionStyle,
                    color = Colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    /**
     * The fill of the arc, measured from its start.
     *
     * The colors are spread over the arc's sweep. The opening below it holds the last color up to
     * its middle and the first color after it, so that each rounded end continues the color of the
     * end it caps.
     */
    private fun arcBrush(colors: List<Color>): Brush {
        val single = colors.singleOrNull()
        if (single != null) return SolidColor(single)
        val sweepFraction = SWEEP_DEGREES / FULL_TURN_DEGREES
        val openingMiddle = (sweepFraction + 1) / 2
        val stops = colors.mapIndexed { index, color ->
            sweepFraction * index / colors.lastIndex to color
        } + listOf(
            openingMiddle to colors.last(),
            openingMiddle to colors.first(),
            1f to colors.first(),
        )
        return Brush.sweepGradient(colorStops = stops.toTypedArray())
    }

    companion object {
        /**
         * The standard thickness of the arc. Larger gauges use a multiple of it.
         */
        val defaultLineWidth: Dp = 5.52.dp

        /**
         * The angle, clockwise from the three o'clock position, at which the arc starts.
         */
        private const val START_ANGLE_DEGREES = 150f

        /**
         * The angle the arc sweeps clockwise from its start to its end.
         */
        private const val SWEEP_DEGREES = 240f

        /**
         * The angle of a full circle.
         */
        private const val FULL_TURN_DEGREES = 360f

        /**
         * The width of the gap around the value's dot, relative to [lineWidth].
         */
        private const val GAP_WIDTH_FRACTION = 0.39f

        /**
         * The color the gap around the value's dot is drawn with. It is fully opaque, so the gap
         * removes the arc beneath it completely; its hue has no effect.
         */
        private val GAP_COLOR = Color.Black

        /**
         * The width and height of a [Size.Regular] gauge.
         */
        private val REGULAR_DIAMETER = 80.dp

        /**
         * The width and height of a [Size.Large] gauge.
         */
        private val LARGE_DIAMETER = 140.dp

        /**
         * How far a [Caption.Description] reaches up into the open bottom of the gauge.
         */
        private val DESCRIPTION_OVERLAP = 8.dp
    }
}

private class GaugePreviewProvider : PreviewParameterProvider<Gauge> {

    private val base = Gauge(
        progress = 0.7f,
        colors = DashboardColorPalette.redToGreen,
        lineWidth = Gauge.defaultLineWidth * 1.5f,
        size = Gauge.Size.Regular,
        label = Gauge.Label(
            value = StringResource("70"),
            caption = Gauge.Caption.Description(text = StringResource("7:36")),
        ),
    )

    override val values: Sequence<Gauge> = sequenceOf(
        base,
        base.copy(label = null),
        base.copy(label = base.label?.copy(caption = null)),
        base.copy(progress = 0f),
        base.copy(progress = 1f),
        base.copy(
            progress = null,
            colors = listOf(ColorToken.Outline),
            label = base.label?.copy(
                value = StringResource("—"),
                caption = Gauge.Caption.Description(text = StringResource("Tap to learn more…")),
            ),
        ),
        base.copy(
            lineWidth = Gauge.defaultLineWidth * 2,
            size = Gauge.Size.Large,
            label = base.label?.copy(
                caption = Gauge.Caption.Range(
                    minimum = StringResource("0"),
                    maximum = StringResource("100"),
                ),
            ),
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(GaugePreviewProvider::class) content: Gauge,
) {
    GroveTheme {
        content.Content(modifier = Modifier.padding(Spacings.medium))
    }
}
