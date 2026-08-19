//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.account

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A DataDisplayComposable for any value type.
 *
 * @param transform A function that takes a value and returns a StringResource to be displayed a the string representation of the value,
 */
data class ValueTextDisplay<Value>(
    val transform: (Value) -> StringResource,
) : DataDisplayComposable<Value> {
    @Composable
    override fun Content(value: Value, modifier: Modifier) {
        AccountValueText(
            text = transform(value).text(),
            modifier = modifier,
        )
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val display = ValueTextDisplay<Boolean> {
        StringResource(if (it) "Accepted" else "Denied")
    }

    GroveTheme {
        Column {
            display.Content(value = true)
            display.Content(value = false)
        }
    }
}
