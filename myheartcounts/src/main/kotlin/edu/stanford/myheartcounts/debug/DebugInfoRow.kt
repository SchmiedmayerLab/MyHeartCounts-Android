//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.debug

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import edu.stanford.myheartcounts.MHCStrings
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.ItemDisplayRowComposable
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A [label] and its display-ready [value] in a debug info card.
 */
data class DebugInfoRow(
    val label: StringResource,
    val value: StringResource,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        ItemDisplayRowComposable(
            modifier = modifier,
            leadingImage = null,
            label = label.text(),
            valueContent = {
                Text(
                    text = value.text(),
                    color = Colors.onSurfaceVariant,
                )
            },
        )
    }
}

private class DebugInfoRowPreviewProvider : PreviewParameterProvider<DebugInfoRow> {
    private val base = DebugInfoRow(
        label = StringResource("Version"),
        value = StringResource("1.2.0"),
    )

    override val values = sequenceOf(
        base,
        base.copy(label = StringResource("Account ID"), value = StringResource(MHCStrings.debug_info_unavailable)),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(DebugInfoRowPreviewProvider::class) content: DebugInfoRow) {
    GroveTheme { content.Content(modifier = Modifier.fillMaxWidth()) }
}
