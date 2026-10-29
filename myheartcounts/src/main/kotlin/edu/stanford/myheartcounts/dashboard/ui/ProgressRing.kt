//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.resolve

/**
 * A ring filling up clockwise from the top as progress is made, optionally with an icon at its
 * center.
 *
 * The whole ring is always drawn as a muted track, with the progress drawn over it. The ring is
 * centered in the space it is given and sized to its smaller dimension.
 *
 * @param progress how far the ring is filled, from `0` for empty to `1` for full; must lie within
 * that range
 * @param progressColor the color of the filled part of the ring
 * @param icon the symbol at the center of the ring, tinted as given; omitted when there is none
 */
@Immutable
data class ProgressRing(
    val progress: Float,
    val progressColor: ColorToken,
    val icon: ImageResource?,
) : ComposableContent {

    init {
        require(progress in 0f..1f) {
            "progress must lie within 0 and 1, but was $progress"
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val trackColor = Colors.outlineVariant
        val fillColor = progressColor.resolve()
        val lineWidth = Sizes.Border.medium
        Box(
            modifier = modifier.semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = progress,
                    range = 0f..1f,
                )
            },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = lineWidth.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val topLeft = Offset(
                    x = center.x - radius,
                    y = center.y - radius,
                )
                val arcSize = Size(
                    width = radius * 2,
                    height = radius * 2,
                )
                drawCircle(
                    color = trackColor,
                    radius = radius,
                    style = Stroke(width = strokeWidth),
                )
                if (progress > 0f) {
                    drawArc(
                        color = fillColor,
                        startAngle = START_ANGLE_DEGREES,
                        sweepAngle = FULL_TURN_DEGREES * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                        ),
                    )
                }
            }
            icon?.Content(modifier = Modifier.size(Sizes.Icon.extraSmall))
        }
    }

    private companion object {
        /**
         * The angle, clockwise from the three o'clock position, at which the progress starts.
         */
        const val START_ANGLE_DEGREES = -90f

        /**
         * The angle of a full circle.
         */
        const val FULL_TURN_DEGREES = 360f
    }
}

private class ProgressRingPreviewProvider : PreviewParameterProvider<ProgressRing> {

    private val base = ProgressRing(
        progress = 0.7f,
        progressColor = DashboardColorPalette.green,
        icon = ImageResource.Vector(
            image = Icons.AutoMirrored.Filled.DirectionsWalk,
            tint = { Colors.onSurface },
        ),
    )

    override val values: Sequence<ProgressRing> = sequenceOf(
        base,
        base.copy(progress = 0f),
        base.copy(progress = 1f),
        base.copy(icon = null),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(ProgressRingPreviewProvider::class) content: ProgressRing,
) {
    GroveTheme {
        content.Content(
            modifier = Modifier
                .padding(Spacings.medium)
                .size(Sizes.Icon.medium),
        )
    }
}
