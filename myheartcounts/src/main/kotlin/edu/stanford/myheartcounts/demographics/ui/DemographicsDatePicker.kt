//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics.ui

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * A dialog for choosing a date, open for as long as it is shown. Dates after [latest] cannot be
 * chosen.
 *
 * @param selection the date chosen so far, if any; the calendar opens on it
 * @param latest the last date that can be chosen
 * @param confirmTitle the title of the button choosing the selected date
 * @param dismissTitle the title of the button closing the dialog without choosing
 * @param onSelect called with the chosen date
 * @param onDismiss called when the dialog closes without choosing
 */
data class DemographicsDatePicker(
    val selection: LocalDate?,
    val latest: LocalDate,
    val confirmTitle: StringResource,
    val dismissTitle: StringResource,
    val onSelect: (LocalDate) -> Unit,
    val onDismiss: () -> Unit,
) : ComposableContent {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(modifier: Modifier) {
        val latestMillis = latest.utcMillis()
        val selectableDates = remember(latestMillis) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= latestMillis

                override fun isSelectableYear(year: Int) = year <= latest.year
            }
        }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = selection?.utcMillis(),
            initialDisplayedMonthMillis = (selection ?: latest).utcMillis(),
            selectableDates = selectableDates,
        )

        DatePickerDialog(
            modifier = modifier,
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    enabled = state.selectedDateMillis != null,
                    onClick = { state.selectedDateMillis?.let { onSelect(it.utcDate()) } },
                ) {
                    Text(text = confirmTitle.text())
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = dismissTitle.text())
                }
            },
        ) {
            DatePicker(state = state)
        }
    }

    private fun LocalDate.utcMillis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun Long.utcDate() = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
}

private class DemographicsDatePickerPreviewProvider : PreviewParameterProvider<DemographicsDatePicker> {
    private val base = DemographicsDatePicker(
        selection = LocalDate.parse("1990-06-15"),
        latest = LocalDate.parse("2026-01-01"),
        confirmTitle = StringResource("OK"),
        dismissTitle = StringResource("Cancel"),
        onSelect = {},
        onDismiss = {},
    )
    override val values = sequenceOf(
        base,
        base.copy(selection = null),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(DemographicsDatePickerPreviewProvider::class) content: DemographicsDatePicker) {
    GroveTheme { content.Content() }
}
