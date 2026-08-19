//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.account

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.grovealliance.ui.theme.Colors

/**
 * Composable for displaying the value of an account item.
 *
 * @param text The text to display.
 * @param modifier The modifier to apply to this layout.
 */
@Composable
fun AccountValueText(
    text: String,
    modifier: Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        color = Colors.onSurfaceVariant,
    )
}
