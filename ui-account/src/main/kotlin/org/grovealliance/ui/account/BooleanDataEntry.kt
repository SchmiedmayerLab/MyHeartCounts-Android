//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews

/**
 * Composable for displaying a data entry with a boolean value.
 *
 * @param description The description of the data entry.
 */
data class BooleanDataEntry(val description: StringResource) : DataEntryComposable<Boolean> {

    @Composable
    override fun Content(value: Boolean, onValueChange: (Boolean) -> Unit, modifier: Modifier) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = description.text(),
                color = Colors.onSurfaceVariant,
            )

            Switch(
                checked = value,
                onCheckedChange = onValueChange,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val entry = BooleanDataEntry(description = StringResource("Accept data collection"))

    GroveTheme {
        entry.Content(
            modifier = Modifier.fillMaxWidth(),
            value = true,
            onValueChange = {},
        )
    }
}
