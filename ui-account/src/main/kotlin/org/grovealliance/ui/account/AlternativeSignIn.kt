//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.grovealliance.resources.Drawables
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * Composable for displaying alternative sign in options.
 *
 * @param divider The [LabeledHorizontalDivider] to display.
 * @param buttons The list of [AsyncTextButton]s to display.
 */
data class AlternativeSignIn(
    val divider: LabeledHorizontalDivider,
    val buttons: List<AsyncTextButton>,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Spacings.medium),
        ) {
            divider.Content(modifier = Modifier.fillMaxWidth())

            buttons.forEach {
                it.Content(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val content = AlternativeSignIn(
        divider = LabeledHorizontalDivider(
            label = StringResource("or"),
        ),
        buttons = listOf(
            AsyncTextButton(
                title = StringResource("Sign in with Google"),
                icon = ImageResource(Drawables.ic_google),
                action = {}
            ),
            AsyncTextButton(
                title = StringResource("Sign in with Stanford"),
                icon = ImageResource(Icons.Default.School),
                action = {}
            )
        )
    )
    GroveTheme {
        content.Content()
    }
}
