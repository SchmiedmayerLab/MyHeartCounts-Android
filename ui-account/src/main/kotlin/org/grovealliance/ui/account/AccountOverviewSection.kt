//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.VerticalSpacer
import org.grovealliance.ui.account.internal.AccountSectionTitle
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * Data class representing a section in the account overview screen.
 *
 * @param title The title of the section.
 * @param items The list of items to be displayed in the section.
 */
data class AccountOverviewSection(
    val title: StringResource?,
    val items: List<AnyAccountOverviewItem>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(modifier = modifier) {
            title?.let {
                AccountSectionTitle(title = it)
                VerticalSpacer(height = Spacings.extraSmall)
            }
            GroveCard {
                items.forEachIndexed { index, item ->
                    item.Content(modifier = Modifier.fillMaxWidth())
                    if (index < items.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacings.medium),
                        )
                    }
                }
            }
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

    GroveTheme {
        section.Content(modifier = Modifier.fillMaxWidth())
    }
}
