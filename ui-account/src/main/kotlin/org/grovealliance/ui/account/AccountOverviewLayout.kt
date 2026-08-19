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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
 * Composable content rendering the overview screen
 *
 * @param title title of the screen
 * @param header header of the screen
 * @param sections list of sections to be rendered
 * @param customSection optional custom content to be rendered between the sections and the logout button
 * @param logout button to logout
 */
data class AccountOverviewLayout(
    val title: StringResource,
    val header: AccountProfileHeader,
    val sections: List<AccountOverviewSection>,
    val customSection: ComposableContent?,
    val logout: AsyncTextButton,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier
                .padding(Spacings.medium)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacings.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            header.Content()

            sections.forEach {
                it.Content(modifier = Modifier.fillMaxWidth())
            }

            customSection?.Content(modifier = Modifier.fillMaxWidth())

            logout.Content(modifier = Modifier.fillMaxWidth())
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val name = AccountOverviewItem(
        title = StringResource("Name"),
        valueDisplay = StringDataDisplay(),
        value = "John Smith",
        leadingImage = null,
        showArrow = true,
        onClick = {},
    )

    val genderIdentity = AccountOverviewItem(
        title = StringResource("Gender identity"),
        valueDisplay = StringDataDisplay(),
        value = "Prefer not to state",
        leadingImage = null,
        showArrow = false,
        onClick = {},
    )

    val section = AccountOverviewSection(
        title = StringResource("NAME"),
        items = listOf(
            name,
            genderIdentity,
        ),
    )
    val layout = AccountOverviewLayout(
        title = StringResource("Account overview"),
        header = AccountProfileHeader(
            initials = "LS",
            name = "Leland Stanford",
            description = "lelandstanford@stanford.edu",
        ),
        sections = listOf(
            section,
        ),
        customSection = null,
        logout = AsyncTextButton(
            title = StringResource("Logout"),
            containerColor = { Colors.error },
            action = {}
        ),
    )

    GroveTheme {
        layout.Content(modifier = Modifier.fillMaxSize())
    }
}
