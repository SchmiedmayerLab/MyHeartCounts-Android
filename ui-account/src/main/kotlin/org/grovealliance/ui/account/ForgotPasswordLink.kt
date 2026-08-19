//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews

/**
 * Composable for displaying a forgot password link.
 *
 * @param text The text to display.
 * @param onClick The action to perform when the link is clicked.
 */
data class ForgotPasswordLink(
    val text: StringResource,
    val onClick: () -> Unit,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        Text(
            modifier = modifier.clickable(onClick = onClick),
            text = text.text(),
            color = Colors.primary,
        )
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    GroveTheme {
        ForgotPasswordLink(
            text = StringResource("Forgot password?"),
            onClick = {}
        ).Content()
    }
}
