//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.entry

import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A segmented choice between input panels.
 *
 * @param options the labels in display order
 * @param selectedIndex the selected segment
 * @param onSelected called when a segment is tapped
 */
@Immutable
data class EntryModePicker(
    val options: List<StringResource>,
    val selectedIndex: StateFlow<Int>,
    val onSelected: (Int) -> Unit,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        val selected by selectedIndex.collectAsStateWithLifecycle()
        SingleChoiceSegmentedButtonRow(modifier = modifier) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = index == selected,
                    onClick = { onSelected(index) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                ) {
                    Text(text = option.text())
                }
            }
        }
    }
}

private class EntryModePickerPreviewProvider : PreviewParameterProvider<EntryModePicker> {
    private val base = EntryModePicker(
        options = listOf(StringResource("BMI"), StringResource("Weight and Height")),
        selectedIndex = MutableStateFlow(1),
        onSelected = {},
    )
    override val values = sequenceOf(base)
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(EntryModePickerPreviewProvider::class) content: EntryModePicker) {
    GroveTheme { content.Content() }
}
