//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A labeled row of independently selected options.
 *
 * @param label the row label
 * @param pickers the inline option pickers
 */
@Immutable
data class EntryOptionsRow(
    val label: StringResource,
    val pickers: List<Picker>,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        Row(
            modifier = modifier.heightIn(min = Sizes.Content.large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacings.small),
        ) {
            EntryRowLabel(text = label.text())
            pickers.forEach { Picker(picker = it) }
        }
    }

    @Composable
    private fun Picker(picker: Picker) {
        val selected by picker.selectedIndex.collectAsStateWithLifecycle()
        var expanded by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(text = picker.options[selected].text())
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                picker.options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(text = option.text()) },
                        onClick = {
                            picker.onSelected(index)
                            expanded = false
                        },
                    )
                }
            }
        }
    }

    /**
     * The options and current selection of one picker.
     */
    data class Picker(
        val options: List<StringResource>,
        val selectedIndex: StateFlow<Int>,
        val onSelected: (Int) -> Unit,
    )
}

private class EntryOptionsRowPreviewProvider : PreviewParameterProvider<EntryOptionsRow> {
    private val base = EntryOptionsRow(
        label = StringResource("Height"),
        pickers = listOf(
            EntryOptionsRow.Picker(
                options = listOf(StringResource("5 ft"), StringResource("6 ft")),
                selectedIndex = MutableStateFlow(0),
                onSelected = {},
            ),
        ),
    )
    override val values = sequenceOf(base)
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(EntryOptionsRowPreviewProvider::class) content: EntryOptionsRow) {
    GroveTheme { content.Content() }
}
