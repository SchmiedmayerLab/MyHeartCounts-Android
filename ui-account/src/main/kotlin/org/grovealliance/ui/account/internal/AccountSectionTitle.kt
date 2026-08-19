//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.account.internal

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.TextStyles

/**
 * Composable Section title in any of the account screens
 *
 * @param title Title of the section
 */
@Composable
internal fun AccountSectionTitle(title: StringResource) {
    Text(
        text = title.text().uppercase(),
        style = TextStyles.bodySmall,
        color = Colors.onSurfaceVariant,
    )
}
