//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A [title] above a [text] in a debug card.
 */
data class DebugTextRow(
    val title: StringResource,
    val text: StringResource,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier.padding(Spacings.medium),
            verticalArrangement = Arrangement.spacedBy(Spacings.extraSmall),
        ) {
            Text(
                text = title.text(),
                style = TextStyles.titleSmall,
                color = Colors.onSurface,
            )
            Text(
                text = text.text(),
                style = TextStyles.bodyMedium,
                color = Colors.onSurfaceVariant,
            )
        }
    }
}

private class DebugTextRowPreviewProvider : PreviewParameterProvider<DebugTextRow> {
    private val base = DebugTextRow(
        title = StringResource("Granted"),
        text = StringResource("Steps (R), Weight (RW)"),
    )

    override val values = sequenceOf(
        base,
        base.copy(title = StringResource("Not granted"), text = StringResource("None")),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(DebugTextRowPreviewProvider::class) content: DebugTextRow) {
    GroveTheme { content.Content(modifier = Modifier.fillMaxWidth()) }
}
