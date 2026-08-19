//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews

/**
 * Composable for displaying a string based account values.
 */
class StringDataDisplay : DataDisplayComposable<String> {
    @Composable
    override fun Content(value: String, modifier: Modifier) {
        AccountValueText(
            text = value,
            modifier = modifier,
        )
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val display = StringDataDisplay()

    GroveTheme {
        display.Content(value = "Hello World")
    }
}
