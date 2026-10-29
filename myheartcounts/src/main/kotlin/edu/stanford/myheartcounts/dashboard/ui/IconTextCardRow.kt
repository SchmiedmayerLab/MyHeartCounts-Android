//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
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
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.resolve

/**
 * A row of an [IconTextCard]: an icon beside a text, which wraps onto as many lines as it needs.
 *
 * The icon sits at the top of the row at the same size in every row, so the texts of rows stacked
 * in a card line up.
 *
 * @param icon the symbol leading the text, tinted as given
 * @param text the text itself
 */
@Immutable
data class IconTextCardRow(
    val icon: ImageResource,
    val text: StringResource,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Row(
            modifier = modifier.semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(Spacings.medium),
        ) {
            icon.Content(modifier = Modifier.size(Sizes.Icon.small))
            Text(
                text = text.text(),
                style = TextStyles.bodyMedium,
                color = Colors.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
            )
        }
    }
}

private class IconTextCardRowPreviewProvider : PreviewParameterProvider<IconTextCardRow> {

    private val base = IconTextCardRow(
        icon = ImageResource.Vector(
            image = Icons.Default.LocalFireDepartment,
            tint = { DashboardColorPalette.orange.resolve() },
        ),
        text = StringResource("You've burned 2,053 active calories — the equivalent of 7 slices of pizza."),
    )

    override val values: Sequence<IconTextCardRow> = sequenceOf(
        base,
        base.copy(text = StringResource("A short text")),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(IconTextCardRowPreviewProvider::class) content: IconTextCardRow,
) {
    GroveTheme {
        content.Content(
            modifier = Modifier
                .padding(Spacings.medium)
                .width(width = 360.dp),
        )
    }
}
