//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.grovealliance.resources.Strings
import java.time.Instant

@Composable
fun DatePickerDialog(
    onDateSelected: (Instant) -> Unit,
    selectableDatesPredicate: (Instant) -> Boolean = { it <= Instant.now() },
    onDismiss: OnActionVoid,
) {
    val datePickerState = rememberDatePickerState(selectableDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            return selectableDatesPredicate(Instant.ofEpochMilli(utcTimeMillis))
        }
    })

    DatePickerDialog(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onDateSelected(Instant.ofEpochMilli(it))
                    }
                    onDismiss()
                }
            ) {
                Text(text = stringResource(Strings.date_picker_confirm_button_title))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(Strings.date_picker_dismiss_button_title))
            }
        },
        content = { DatePicker(state = datePickerState) }
    )
}
