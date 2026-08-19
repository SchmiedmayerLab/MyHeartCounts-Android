//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * Layout displaying an icon and a single section of tappable account overview rows.
 *
 * @param icon Icon shown at the top of the layout.
 * @param section Rows to display.
 */
data class AccountSectionOverviewLayout(
    val icon: ImageResource,
    val section: AccountOverviewSection,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier
                .padding(Spacings.medium)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacings.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            icon.Content(modifier = Modifier.size(Sizes.Icon.medium))
            section.Content(modifier = Modifier.fillMaxWidth())
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val nameRow = AccountOverviewItem(
        title = StringResource("Full Name"),
        valueDisplay = StringDataDisplay(),
        value = "Leland Stanford",
        leadingImage = null,
        showArrow = true,
        onClick = {},
    )
    val emailRow = AccountOverviewItem(
        title = StringResource("Email Address"),
        valueDisplay = StringDataDisplay(),
        value = "lelandstanford@stanford.edu",
        leadingImage = null,
        showArrow = true,
        onClick = {},
    )
    val layout = AccountSectionOverviewLayout(
        section = AccountOverviewSection(
            title = null,
            items = listOf(nameRow, emailRow),
        ),
        icon = ImageResource(Icons.Default.AccountCircle),
    )
    GroveTheme {
        layout.Content(modifier = Modifier.fillMaxSize())
    }
}
