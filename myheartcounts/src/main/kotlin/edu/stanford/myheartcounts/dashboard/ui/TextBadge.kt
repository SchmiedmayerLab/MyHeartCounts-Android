//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
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

/**
 * A short text on a small rounded background, both in the same color, the background faint.
 *
 * @param text the text itself, already formatted
 * @param color the color of the text and, faintly, of the background
 */
@Immutable
data class TextBadge(
    val text: StringResource,
    val color: ColorToken,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val resolvedColor = color.resolve()
        Text(
            text = text.text(),
            style = TextStyles.labelMedium.medium(),
            color = resolvedColor,
            maxLines = 1,
            modifier = modifier
                .background(
                    color = resolvedColor.copy(alpha = BACKGROUND_ALPHA),
                    shape = GroveShapes.regular,
                )
                .padding(Spacings.extraSmall),
        )
    }

    private companion object {
        /**
         * The opacity of the background, relative to [color].
         */
        const val BACKGROUND_ALPHA = 0.15f
    }
}

private class TextBadgePreviewProvider : PreviewParameterProvider<TextBadge> {

    private val base = TextBadge(
        text = StringResource("45%"),
        color = DashboardColorPalette.green,
    )

    override val values: Sequence<TextBadge> = sequenceOf(
        base,
        base.copy(color = ColorToken.Primary),
        base.copy(text = StringResource("A longer badge")),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(TextBadgePreviewProvider::class) content: TextBadge,
) {
    GroveTheme {
        content.Content(modifier = Modifier.padding(Spacings.medium))
    }
}
