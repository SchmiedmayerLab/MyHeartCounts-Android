//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.debug

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
 * The uppercase [title] above a section of the debug sheet.
 */
data class DebugSectionTitle(
    val title: StringResource,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Text(
            modifier = modifier.padding(top = Spacings.small),
            text = title.text().uppercase(),
            style = TextStyles.labelLarge,
            color = Colors.onSurfaceVariant,
        )
    }
}

private class DebugSectionTitlePreviewProvider : PreviewParameterProvider<DebugSectionTitle> {
    private val base = DebugSectionTitle(title = StringResource("App Info"))

    override val values = sequenceOf(base)
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(DebugSectionTitlePreviewProvider::class) content: DebugSectionTitle) {
    GroveTheme { content.Content() }
}
