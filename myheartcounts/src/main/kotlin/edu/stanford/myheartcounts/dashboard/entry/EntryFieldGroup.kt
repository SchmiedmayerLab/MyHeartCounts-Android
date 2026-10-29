//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.entry

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews

/**
 * Input rows separated by dividers, optionally followed by inline choice pickers.
 *
 * @param rows the labeled input rows
 * @param options the optional row of choice pickers
 */
@Immutable
data class EntryFieldGroup(
    val rows: List<EntryFieldRow>,
    val options: EntryOptionsRow?,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        Column(modifier = modifier) {
            rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(color = Colors.outlineVariant)
                row.Content()
            }
            options?.let {
                HorizontalDivider(color = Colors.outlineVariant)
                it.Content()
            }
        }
    }
}

private class EntryFieldGroupPreviewProvider : PreviewParameterProvider<EntryFieldGroup> {
    private val base = EntryFieldGroup(
        rows = listOf(
            EntryFieldRow(
                label = StringResource("Systolic Blood Pressure"),
                value = MutableStateFlow("120"),
                placeholder = StringResource("0"),
                unit = StringResource("mmHg"),
                keyboardType = KeyboardType.Number,
                validationMessage = MutableStateFlow(null),
                onValueChange = {},
            ),
        ),
        options = null,
    )
    override val values = sequenceOf(base)
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(EntryFieldGroupPreviewProvider::class) content: EntryFieldGroup) {
    GroveTheme { content.Content() }
}
