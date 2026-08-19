//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.ItemDisplayRowComposable
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews

/**
 * Data class representing an item in the account overview screen.
 *
 * @param title The title of the item.
 * @param valueDisplay The composable to display the value of the item.
 * @param value The value of the item.
 * @param leadingImage The leading image of the item.
 * @param showArrow Whether to show an arrow on the right side of the item.
 * @param onClick The action to perform when the item is clicked
 */
data class AccountOverviewItem<Value>(
    val title: StringResource,
    val valueDisplay: DataDisplayComposable<Value>,
    val value: Value,
    val leadingImage: ImageResource?,
    val showArrow: Boolean = false,
    val onClick: (() -> Unit)? = null,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        ItemDisplayRowComposable(
            modifier = modifier,
            leadingImage = leadingImage,
            label = title.text(),
            showArrow = showArrow,
            onClick = onClick,
            valueContent = { valueDisplay.Content(value = value) }
        )
    }
}

/**
 * Alias for [AccountOverviewItem] with any value type.
 */
typealias AnyAccountOverviewItem = AccountOverviewItem<*>

@ThemePreviews
@Composable
private fun Preview() {
    val item = AccountOverviewItem(
        title = StringResource("Your account"),
        valueDisplay = StringDataDisplay(),
        value = "john.smith2",
        leadingImage = null,
        showArrow = true,
        onClick = {},
    )

    GroveTheme {
        item.Content(modifier = Modifier.fillMaxWidth())
    }
}
