//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * Composable for displaying a sign up link.
 *
 * @param infoText The text to display before the link.
 * @param signUpText The text to display for the link.
 * @param onClick The action to perform when the link is clicked.
 */
data class SignUpLink(
    val infoText: StringResource,
    val signUpText: StringResource,
    val onClick: () -> Unit,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacings.small),
        ) {
            Text(text = infoText.text())
            Text(
                modifier = Modifier.clickable(onClick = onClick),
                text = signUpText.text(),
                color = Colors.primary,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    GroveTheme {
        SignUpLink(
            infoText = StringResource("Don't have an account yet?"),
            signUpText = StringResource("Sign up"),
            onClick = {}
        ).Content()
    }
}
