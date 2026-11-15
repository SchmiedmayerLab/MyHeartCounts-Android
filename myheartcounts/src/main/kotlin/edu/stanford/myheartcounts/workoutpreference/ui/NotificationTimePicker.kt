//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.workoutpreference.ui

import android.text.format.DateFormat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A dialog for choosing a time of day, open for as long as it is shown. The clock follows the
 * device's 12- or 24-hour setting.
 *
 * @param selection the time the clock opens on
 * @param confirmTitle the title of the button choosing the selected time
 * @param dismissTitle the title of the button closing the dialog without choosing
 * @param onSelect called with the chosen time
 * @param onDismiss called when the dialog closes without choosing
 */
data class NotificationTimePicker(
    val selection: State,
    val confirmTitle: StringResource,
    val dismissTitle: StringResource,
    val onSelect: (State) -> Unit,
    val onDismiss: () -> Unit,
) : ComposableContent {

    /**
     * A time of day on the clock.
     *
     * @param hour the hour, from 0 to 23
     * @param minute the minute, from 0 to 59
     */
    data class State(
        val hour: Int,
        val minute: Int,
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(modifier: Modifier) {
        val state = rememberTimePickerState(
            initialHour = selection.hour,
            initialMinute = selection.minute,
            is24Hour = DateFormat.is24HourFormat(LocalContext.current),
        )

        AlertDialog(
            modifier = modifier,
            onDismissRequest = onDismiss,
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSelect(
                            State(
                                hour = state.hour,
                                minute = state.minute,
                            ),
                        )
                    },
                ) {
                    Text(text = confirmTitle.text())
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = dismissTitle.text())
                }
            },
        )
    }
}

private class NotificationTimePickerPreviewProvider : PreviewParameterProvider<NotificationTimePicker> {
    private val base = NotificationTimePicker(
        selection = NotificationTimePicker.State(
            hour = 9,
            minute = 0,
        ),
        confirmTitle = StringResource("OK"),
        dismissTitle = StringResource("Cancel"),
        onSelect = {},
        onDismiss = {},
    )
    override val values = sequenceOf(
        base,
        base.copy(
            selection = NotificationTimePicker.State(
                hour = 18,
                minute = 45,
            ),
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(NotificationTimePickerPreviewProvider::class) content: NotificationTimePicker) {
    GroveTheme { content.Content() }
}
