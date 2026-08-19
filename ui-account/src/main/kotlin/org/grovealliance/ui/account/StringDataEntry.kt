//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.grovealliance.ui.GroveInputFieldComposable
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A data entry composable for string based account values
 *
 * @param placeholder The placeholder text to display when the field is empty.
 * @param hideContent Whether to hide the content of the field, e.g. password fields.
 */
data class StringDataEntry(
    val placeholder: StringResource?,
    val hideContent: Boolean = false,
) : DataEntryComposable<String> {

    @Composable
    override fun Content(value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
        GroveInputFieldComposable(
            value = value,
            onValueChanged = onValueChange,
            modifier = modifier,
            hideContent = hideContent,
            placeholder = placeholder?.text(),
        )
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val entry = StringDataEntry(placeholder = StringResource("Enter your email"))

    GroveTheme {
        entry.Content(value = "email@stanford.edu", onValueChange = {})
    }
}
