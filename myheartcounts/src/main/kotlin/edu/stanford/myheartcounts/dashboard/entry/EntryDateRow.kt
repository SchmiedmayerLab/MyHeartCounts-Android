//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.DatePickerDialog
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews
import java.time.Instant

/**
 * A date row whose date and time read as tappable badges, opening a calendar picker and a time input dialog.
 *
 * @param label the row label
 * @param date the formatted date
 * @param time the formatted time and picker values
 * @param dialog the time dialog labels
 * @param isSelectableDate whether the calendar offers a day, given as the start of that day in UTC
 * @param onDateSelected called when a calendar date is selected
 * @param onTimeSelected called when an hour and minute are selected
 */
@Immutable
data class EntryDateRow(
    val label: StringResource,
    val date: StateFlow<StringResource>,
    val time: StateFlow<Time>,
    val dialog: DialogLabels,
    val isSelectableDate: (Instant) -> Boolean,
    val onDateSelected: (Instant) -> Unit,
    val onTimeSelected: (Int, Int) -> Unit,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        Row(
            modifier = modifier.heightIn(min = Sizes.Content.large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacings.small),
        ) {
            EntryRowLabel(text = label.text())
            Date()
            Time()
        }
    }

    @Composable
    private fun Date() {
        val text by date.collectAsStateWithLifecycle()
        var expanded by remember { mutableStateOf(false) }
        ValueBadge(text = text.text(), onClick = { expanded = true })
        if (expanded) {
            DatePickerDialog(
                onDateSelected = onDateSelected,
                selectableDatesPredicate = isSelectableDate,
                onDismiss = { expanded = false },
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Time() {
        val current by time.collectAsStateWithLifecycle()
        var expanded by remember { mutableStateOf(false) }
        ValueBadge(text = current.label.text(), onClick = { expanded = true })
        if (expanded) {
            val picker = rememberTimePickerState(
                initialHour = current.hour,
                initialMinute = current.minute,
                is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current),
            )
            AlertDialog(
                onDismissRequest = { expanded = false },
                title = { Text(text = dialog.title.text()) },
                text = { TimeInput(state = picker) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onTimeSelected(picker.hour, picker.minute)
                            expanded = false
                        },
                    ) {
                        Text(text = dialog.confirm.text())
                    }
                },
                dismissButton = {
                    TextButton(onClick = { expanded = false }) { Text(text = dialog.cancel.text()) }
                },
            )
        }
    }

    @Composable
    private fun ValueBadge(text: String, onClick: () -> Unit) {
        Text(
            text = text,
            style = TextStyles.bodyLarge,
            color = Colors.onSurface,
            maxLines = 1,
            modifier = Modifier
                .clip(GroveShapes.regular)
                .background(color = Colors.surfaceContainerHigh)
                .clickable(onClick = onClick)
                .padding(
                    horizontal = Spacings.small,
                    vertical = Spacings.extraSmall,
                ),
        )
    }

    /**
     * The labels of the time input dialog.
     */
    data class DialogLabels(
        val title: StringResource,
        val confirm: StringResource,
        val cancel: StringResource,
    )

    /**
     * A formatted time and the hour/minute shown when its picker opens.
     */
    data class Time(val label: StringResource, val hour: Int, val minute: Int)
}

private class EntryDateRowPreviewProvider : PreviewParameterProvider<EntryDateRow> {
    private val base = EntryDateRow(
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
    )
    override val values = sequenceOf(base)
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(EntryDateRowPreviewProvider::class) content: EntryDateRow) {
    GroveTheme { content.Content() }
}
