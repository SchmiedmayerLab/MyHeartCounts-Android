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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.bold
import org.grovealliance.ui.theme.medium
import org.grovealliance.ui.theme.resolve
import org.grovealliance.ui.theme.semiBold

/**
 * A tile showing a single value under a titled icon, with an optional footnote below it.
 *
 * The title is drawn in capitals on a single line. Without a value, a muted dash stands in for it.
 * The line of the footnote is kept even without one, so tiles side by side stay equally tall.
 *
 * @param icon the symbol leading the title, tinted as given
 * @param title what the value describes
 * @param value the value itself; omitted when there is none yet
 * @param footnote a qualifier shown below the value; omitted when there is none
 */
@Immutable
data class StatTile(
    val icon: ImageResource,
    val title: StringResource,
    val value: Value?,
    val footnote: StringResource?,
) : ComposableContent {

    /**
     * The value of a [StatTile].
     *
     * @param text the value, already formatted
     * @param unit the unit following the value, drawn smaller and muted; omitted when the text
     * carries its own unit or there is none
     */
    data class Value(
        val text: StringResource,
        val unit: StringResource?,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val valueStyle = TextStyles.titleLarge.bold()
        GroveCard(
            modifier = modifier.semantics(mergeDescendants = true) {},
            shape = GroveShapes.large,
        ) {
            Column(
                modifier = Modifier.padding(Spacings.medium),
                verticalArrangement = Arrangement.spacedBy(Spacings.small),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacings.small),
                ) {
                    icon.Content(modifier = Modifier.size(Sizes.Icon.extraSmall))
                    Text(
                        text = title.text().toUpperCase(LocaleList.current),
                        style = TextStyles.labelMedium.semiBold(),
                        color = Colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (value != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacings.extraSmall)) {
                        Text(
                            text = value.text.text(),
                            style = valueStyle,
                            color = Colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .alignByBaseline()
                                .weight(weight = 1f, fill = false),
                        )
                        if (value.unit != null) {
                            Text(
                                text = value.unit.text(),
                                style = TextStyles.titleMedium.medium(),
                                color = Colors.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.alignByBaseline(),
                            )
                        }
                    }
                } else {
                    Text(
                        text = PLACEHOLDER,
                        style = valueStyle,
                        color = Colors.outline,
                    )
                }
                Text(
                    text = footnote?.text().orEmpty(),
                    style = TextStyles.labelSmall,
                    color = Colors.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    private companion object {
        /**
         * The dash standing in for a missing value.
         */
        const val PLACEHOLDER = "—"
    }
}

private class StatTilePreviewProvider : PreviewParameterProvider<StatTile> {

    private val base = StatTile(
        icon = ImageResource.Vector(
            image = Icons.Default.Favorite,
            tint = { DashboardColorPalette.pink.resolve() },
        ),
        title = StringResource("Resting HR"),
        value = StatTile.Value(
            text = StringResource("80"),
            unit = StringResource("bpm"),
        ),
        footnote = StringResource("Average"),
    )

    override val values: Sequence<StatTile> = sequenceOf(
        base,
        base.copy(value = base.value?.copy(unit = null)),
        base.copy(footnote = null),
        base.copy(value = null),
        base.copy(title = StringResource("A title too long for a single line")),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(StatTilePreviewProvider::class) content: StatTile,
) {
    GroveTheme {
        content.Content(
            modifier = Modifier
                .padding(Spacings.medium)
                .width(width = 180.dp),
        )
    }
}
