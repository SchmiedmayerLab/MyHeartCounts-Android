//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.toUpperCase
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
 * A stack of [ProgressRingRow]s under an optional label, drawn without a surface of its own so it
 * can sit inside a card.
 *
 * @param label the heading above the rows, drawn in capitals; omitted when there is none
 * @param rows the rows, from top to bottom
 */
@Immutable
data class ProgressRingRowList(
    val label: StringResource?,
    val rows: List<ProgressRingRow>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Spacings.small),
        ) {
            if (label != null) {
                Text(
                    text = label.text().toUpperCase(LocaleList.current),
                    style = TextStyles.labelMedium,
                    color = Colors.onSurfaceVariant,
                    modifier = Modifier.semantics { heading() },
                )
            }
            rows.forEach { row ->
                row.Content(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private class ProgressRingRowListPreviewProvider : PreviewParameterProvider<ProgressRingRowList> {

    private val row = ProgressRingRow(
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

    private val base = ProgressRingRowList(
        label = StringResource("Label"),
        rows = listOf(
            row,
            row.copy(
                ring = row.ring.copy(
                    progress = 0f,
                    icon = ImageResource.Vector(
                        image = Icons.AutoMirrored.Filled.Assignment,
                        tint = { Colors.onSurface },
                    ),
                ),
                title = StringResource("Another title"),
            ),
            row.copy(
                ring = row.ring.copy(progress = 0f),
                title = StringResource("A third title"),
            ),
        ),
    )

    override val values: Sequence<ProgressRingRowList> = sequenceOf(
        base,
        base.copy(label = null),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(ProgressRingRowListPreviewProvider::class) content: ProgressRingRowList,
) {
    GroveTheme {
        content.Content(
            modifier = Modifier
                .padding(Spacings.medium)
                .width(width = 360.dp),
        )
    }
}
