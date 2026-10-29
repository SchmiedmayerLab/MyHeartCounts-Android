//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.bold
import org.grovealliance.ui.theme.medium

/**
 * A card led by a large counter, with optional details below it.
 *
 * The counter and its unit share a baseline, with a badge in the top end corner. Below them, a
 * labeled value sits beside a row when space permits, wrapping above it otherwise. A list follows
 * under a divider. Any of these parts
 * is left out when omitted.
 *
 * @param value the counter itself, already formatted
 * @param unit what the counter counts, drawn smaller and muted after it; omitted when there is none
 * @param badge the badge in the top end corner; omitted when there is none
 * @param detail the labeled value below the counter; omitted when there is none
 * @param trailingRow the row beside or below [detail], depending on available width; omitted when there is none
 * @param list the rows below a divider at the bottom of the card; omitted when there are none
 * @param onClick invoked when the card is tapped; the card is not tappable when omitted
 */
@Immutable
data class CounterCard(
    val value: StringResource,
    val unit: StringResource?,
    val badge: ProgressBadge?,
    val detail: LabeledValue?,
    val trailingRow: ProgressRingRow?,
    val list: ProgressRingRowList?,
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
                modifier = Modifier.padding(Spacings.medium),
                verticalArrangement = Arrangement.spacedBy(Spacings.medium),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacings.small)) {
                        Text(
                            text = value.text(),
                            style = TextStyles.displayLarge.bold(),
                            color = Colors.onSurface,
                            maxLines = 1,
                            modifier = Modifier.alignByBaseline(),
                        )
                        if (unit != null) {
                            Text(
                                text = unit.text(),
                                style = TextStyles.titleLarge.medium(),
                                color = Colors.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.alignByBaseline(),
                            )
                        }
                    }
                    badge?.Content(modifier = Modifier.align(Alignment.TopEnd))
                }
                if (detail != null || trailingRow != null) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacings.medium),
                        verticalArrangement = Arrangement.spacedBy(Spacings.medium),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        detail?.Content(modifier = Modifier.width(IntrinsicSize.Max))
                        trailingRow?.Content(modifier = Modifier.weight(1f))
                    }
                }
                if (list != null) {
                    HorizontalDivider()
                    list.Content(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

private class CounterCardPreviewProvider : PreviewParameterProvider<CounterCard> {

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

    private val base = CounterCard(
        value = StringResource("6"),
        unit = StringResource("days"),
        badge = ProgressBadge(
            icon = ImageResource.Vector(
                image = Icons.Default.MilitaryTech,
                tint = { Colors.onSurface },
            ),
            label = StringResource("5 / 20"),
            progress = 0.25f,
            progressColor = ColorToken.Primary,
        ),
        detail = LabeledValue(
            label = StringResource("Label"),
            value = StringResource("20 October 2026"),
        ),
        trailingRow = row.copy(
            ring = row.ring.copy(
                progress = 0f,
                icon = ImageResource.Vector(
                    image = Icons.Default.CalendarMonth,
                    tint = { Colors.onSurface },
                ),
            ),
            title = StringResource("Next title"),
            subtitle = StringResource("Next subtitle"),
        ),
        list = ProgressRingRowList(
            label = StringResource("List label"),
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
            ),
        ),
        onClick = {},
    )

    override val values: Sequence<CounterCard> = sequenceOf(
        base,
        base.copy(list = null),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(CounterCardPreviewProvider::class) content: CounterCard,
) {
    GroveTheme {
        content.Content(modifier = Modifier.padding(Spacings.medium))
    }
}
