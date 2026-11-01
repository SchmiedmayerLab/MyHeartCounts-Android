//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.bold

/**
 * A titled form with a mode selector, a grouped date and input rows, and inline feedback.
 *
 * @param title the form heading
 * @param picker the optional panel selector
 * @param dateRow the date and time controls
 * @param panels the available groups of fields
 * @param selectedPanel the index of the visible group
 * @param feedback optional inline information and errors
 */
@Immutable
data class EntryFormLayout(
    val title: StringResource,
    val picker: EntryModePicker?,
    val dateRow: EntryDateRow,
    val panels: List<EntryFieldGroup>,
    val selectedPanel: StateFlow<Int>,
    val feedback: Feedback,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier.verticalScroll(rememberScrollState()).padding(Spacings.medium),
            verticalArrangement = Arrangement.spacedBy(Spacings.medium),
        ) {
            Text(text = title.text(), style = TextStyles.headlineLarge.bold())
            picker?.Content(modifier = Modifier.fillMaxWidth())
            GroveCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = Spacings.medium)) {
                    dateRow.Content(modifier = Modifier.fillMaxWidth())
                    HorizontalDivider(color = Colors.outlineVariant)
                    SelectedPanel()
                }
            }
            Footnote()
            Error()
        }
    }

    @Composable
    private fun SelectedPanel() {
        val index by selectedPanel.collectAsStateWithLifecycle()
        panels[index].Content(modifier = Modifier.fillMaxWidth())
    }

    @Composable
    private fun Footnote() {
        val text by feedback.footnote.collectAsStateWithLifecycle()
        text?.let {
            Text(text = it.text(), style = TextStyles.bodySmall, color = Colors.onSurfaceVariant)
        }
    }

    @Composable
    private fun Error() {
        val text by feedback.error.collectAsStateWithLifecycle()
        text?.let { Text(text = it.text(), style = TextStyles.bodySmall, color = Colors.error) }
    }

    /**
     * Optional inline information and validation text.
     */
    data class Feedback(
        val footnote: StateFlow<StringResource?>,
        val error: StateFlow<StringResource?>,
    )
}

private class EntryFormLayoutPreviewProvider : PreviewParameterProvider<EntryFormLayout> {
    private val base = EntryFormLayout(
        title = StringResource("Enter Blood Pressure"),
        picker = null,
        dateRow = EntryDateRow(
            label = StringResource("Date"),
            date = MutableStateFlow(StringResource("Jan 1, 2026")),
            time = MutableStateFlow(
                EntryDateRow.Time(
                    label = StringResource("01:02"),
                    hour = 1,
                    minute = 2,
                ),
            ),
            dialog = EntryDateRow.DialogLabels(
                title = StringResource("Time"),
                confirm = StringResource("OK"),
                cancel = StringResource("Cancel"),
            ),
            isSelectableDate = { true },
            onDateSelected = {},
            onTimeSelected = { _, _ -> },
        ),
        panels = listOf(
            EntryFieldGroup(
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
            ),
        ),
        selectedPanel = MutableStateFlow(0),
        feedback = EntryFormLayout.Feedback(
            footnote = MutableStateFlow(null),
            error = MutableStateFlow(null),
        ),
    )
    override val values = sequenceOf(base)
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(EntryFormLayoutPreviewProvider::class) content: EntryFormLayout) {
    GroveTheme { content.Content() }
}
