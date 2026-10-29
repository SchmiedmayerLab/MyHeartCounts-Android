//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A [ProgressRing] beside a title, with an optional subtitle below the title and an optional badge
 * at the end.
 *
 * @param ring the ring leading the row
 * @param title what the ring tracks
 * @param subtitle a description below the title, drawn muted; omitted when there is none
 * @param trailingBadge the badge at the end of the row; omitted when there is none
 */
@Immutable
data class ProgressRingRow(
    val ring: ProgressRing,
    val title: StringResource,
    val subtitle: StringResource?,
    val trailingBadge: TextBadge?,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Row(
            modifier = modifier.semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(Spacings.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ring.Content(modifier = Modifier.size(RING_SIZE))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.text(),
                    style = TextStyles.bodyMedium,
                    color = Colors.onSurface,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle.text(),
                        style = TextStyles.bodySmall,
                        color = Colors.onSurfaceVariant,
                    )
                }
            }
            trailingBadge?.Content()
        }
    }

    private companion object {
        /**
         * The diameter of achievement rings beside a row's text.
         */
        val RING_SIZE = 40.dp
    }
}

private class ProgressRingRowPreviewProvider : PreviewParameterProvider<ProgressRingRow> {

    private val base = ProgressRingRow(
        ring = ProgressRing(
            progress = 0.7f,
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

    override val values: Sequence<ProgressRingRow> = sequenceOf(
        base,
        base.copy(subtitle = null),
        base.copy(ring = base.ring.copy(progress = 0f)),
        base.copy(subtitle = StringResource("A subtitle long enough to wrap onto a second line of the row")),
        base.copy(
            trailingBadge = TextBadge(
                text = StringResource("70%"),
                color = DashboardColorPalette.green,
            ),
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(ProgressRingRowPreviewProvider::class) content: ProgressRingRow,
) {
    GroveTheme {
        content.Content(
            modifier = Modifier
                .padding(Spacings.medium)
                .width(width = 360.dp),
        )
    }
}
