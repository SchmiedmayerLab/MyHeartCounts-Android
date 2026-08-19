//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * Displays the actions for an onboarding page.
 *
 * @property primaryButton Main action, rendered as a full-width button.
 * @property secondaryButton Optional supporting action shown below [primaryButton].
 */
class OnboardingActions(
    val primaryButton: AsyncTextButton,
    val secondaryButton: AsyncTextButton? = null,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacings.extraSmall),
        ) {
            primaryButton.Content(modifier = Modifier.fillMaxWidth())
            secondaryButton?.Content()
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    GroveTheme {
        OnboardingActions(
            primaryButton = AsyncTextButton(
                title = StringResource("Get Started"),
                action = {},
            ),
            secondaryButton = AsyncTextButton(
                title = StringResource("Learn More"),
                containerColor = { Colors.transparent },
                textColor = { Colors.primary },
                action = {},
            ),
        ).Content()
    }
}
