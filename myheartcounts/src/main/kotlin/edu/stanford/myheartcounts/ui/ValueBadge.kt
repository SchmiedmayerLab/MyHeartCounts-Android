//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A value on a rounded background that reads as tappable, such as a date or time opening its picker.
 *
 * @param text the value, already formatted
 * @param onClick called when the badge is tapped
 */
data class ValueBadge(
    val text: StringResource,
    val onClick: () -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Text(
            text = text.text(),
            style = TextStyles.bodyLarge,
            color = Colors.onSurface,
            maxLines = 1,
            modifier = modifier
                .clip(GroveShapes.regular)
                .background(color = Colors.surfaceContainerHigh)
                .clickable(onClick = onClick)
                .padding(
                    horizontal = Spacings.small,
                    vertical = Spacings.extraSmall,
                ),
        )
    }
}

private class ValueBadgePreviewProvider : PreviewParameterProvider<ValueBadge> {
    private val base = ValueBadge(
        text = StringResource("09:00"),
        onClick = {},
    )
    override val values = sequenceOf(
        base,
        base.copy(text = StringResource("Jan 1, 2026")),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(ValueBadgePreviewProvider::class) content: ValueBadge) {
    GroveTheme {
        content.Content(modifier = Modifier.padding(Spacings.medium))
    }
}
