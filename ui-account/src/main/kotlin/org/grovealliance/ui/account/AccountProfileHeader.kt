//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.bold

/**
 * Composable content rendering the header of the account overview screen.
 *
 * @param initials Optional initials of the user.
 * @param name Name of the user.
 * @param description Description text of the user
 */
data class AccountProfileHeader(
    val initials: String?,
    val name: String,
    val description: String,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Spacings.small),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            initials?.let {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(Colors.primaryContainer, GroveShapes.circle),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = it,
                        style = TextStyles.headlineLarge.bold(),
                        color = Colors.onPrimaryContainer,
                    )
                }
            }

            Text(
                text = name,
                style = TextStyles.titleLarge.bold(),
            )

            Text(
                text = description,
                style = TextStyles.bodyMedium,
                color = Colors.onSurfaceVariant,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val header = AccountProfileHeader(
        initials = "LS",
        name = "Leland Stanford",
        description = "lelandstanford@stanford.edu"
    )

    GroveTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            header.Content()
        }
    }
}
