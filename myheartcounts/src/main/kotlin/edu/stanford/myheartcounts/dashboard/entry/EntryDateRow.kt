//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.myheartcounts.ui.ValueBadge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.DatePickerDialog
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews
import java.time.Instant

/**
 * A date row whose [date] and [time] read as tappable badges, with the calendar or the time input
 * dialog open while [picker] says so.
 *
 * @param label the row label
 * @param date the formatted date, tapped to open the calendar
 * @param time the formatted time, tapped to open the time input
 * @param picker the dialog currently open
 * @param dialog the time dialog labels
 * @param isSelectableDate whether the calendar offers a day, given as the start of that day in UTC
 * @param onDateSelected called when a calendar date is selected
 * @param onTimeSelected called when an hour and minute are selected
 * @param onPickerDismissed called when the open dialog closes without a selection
 */
@Immutable
data class EntryDateRow(
    val label: StringResource,
    val date: StateFlow<ValueBadge>,
    val time: StateFlow<ValueBadge>,
    val picker: StateFlow<Picker?>,
    val dialog: DialogLabels,
    val isSelectableDate: (Instant) -> Boolean,
    val onDateSelected: (Instant) -> Unit,
    val onTimeSelected: (Int, Int) -> Unit,
    val onPickerDismissed: () -> Unit,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        val date by date.collectAsStateWithLifecycle()
        val time by time.collectAsStateWithLifecycle()
        val picker by picker.collectAsStateWithLifecycle()

        Row(
            modifier = modifier.heightIn(min = Sizes.Content.large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacings.small),
        ) {
            EntryRowLabel(text = label.text())
            date.Content()
            time.Content()
        }
        when (val open = picker) {
            Picker.Date -> DatePickerDialog(
                onDateSelected = onDateSelected,
                selectableDatesPredicate = isSelectableDate,
                onDismiss = onPickerDismissed,
            )
            is Picker.Time -> TimeDialog(time = open)
            null -> Unit
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TimeDialog(time: Picker.Time) {
        val state = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current),
        )
        AlertDialog(
            onDismissRequest = onPickerDismissed,
            title = { Text(text = dialog.title.text()) },
            text = { TimeInput(state = state) },
            confirmButton = {
                TextButton(onClick = { onTimeSelected(state.hour, state.minute) }) {
                    Text(text = dialog.confirm.text())
                }
            },
            dismissButton = {
                TextButton(onClick = onPickerDismissed) { Text(text = dialog.cancel.text()) }
            },
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
     * A dialog of the row.
     */
    sealed interface Picker {

        /**
         * The calendar.
         */
        data object Date : Picker

        /**
         * The time input, opening on [hour] and [minute].
         */
        data class Time(
            val hour: Int,
            val minute: Int,
        ) : Picker
    }
}

private class EntryDateRowPreviewProvider : PreviewParameterProvider<EntryDateRow> {
    private val base = EntryDateRow(
        label = StringResource("Date"),
        date = MutableStateFlow(
            ValueBadge(
                text = StringResource("Jan 1, 2026"),
                onClick = {},
            ),
        ),
        time = MutableStateFlow(
            ValueBadge(
                text = StringResource("01:02"),
                onClick = {},
            ),
        ),
        picker = MutableStateFlow(null),
        dialog = EntryDateRow.DialogLabels(
            title = StringResource("Time"),
            confirm = StringResource("OK"),
            cancel = StringResource("Cancel"),
        ),
        isSelectableDate = { true },
        onDateSelected = {},
        onTimeSelected = { _, _ -> },
        onPickerDismissed = {},
    )
    override val values = sequenceOf(
        base,
        base.copy(
            picker = MutableStateFlow(
                EntryDateRow.Picker.Time(
                    hour = 1,
                    minute = 2,
                ),
            ),
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(EntryDateRowPreviewProvider::class) content: EntryDateRow) {
    GroveTheme { content.Content() }
}
