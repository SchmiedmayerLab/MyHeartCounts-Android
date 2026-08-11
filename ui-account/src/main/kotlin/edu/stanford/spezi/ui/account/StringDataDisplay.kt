//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import edu.stanford.spezi.ui.theme.SpeziTheme
import edu.stanford.spezi.ui.theme.ThemePreviews

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

    SpeziTheme {
        display.Content(value = "Hello World")
    }
}
