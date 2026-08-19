//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.grovealliance.ui.DatePickerFormFieldItemComposable
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews
import java.time.Instant

/**
 * A data entry composable for [Instant] based values, e.g. birth dates.
 *
 * @param placeholder The placeholder text to display when the field is empty.
 * @param formatter A function that takes an [Instant] and returns a formatted string.
 */
data class InstantDataEntry(
    val placeholder: StringResource,
    val formatter: (Instant) -> StringResource,
) : DataEntryComposable<Instant> {
    @Composable
    override fun Content(value: Instant, onValueChange: (Instant) -> Unit, modifier: Modifier) {
        DatePickerFormFieldItemComposable(
            placeholder = placeholder,
            onValueChange = onValueChange,
            value = formatter(value).text(),
            modifier = modifier,
        )
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val entry = InstantDataEntry(
        placeholder = StringResource("Enter your birthdate"),
        formatter = { StringResource("01.01.2026") },
    )
    GroveTheme {
        entry.Content(value = Instant.now(), onValueChange = {}, modifier = Modifier.fillMaxWidth())
    }
}
