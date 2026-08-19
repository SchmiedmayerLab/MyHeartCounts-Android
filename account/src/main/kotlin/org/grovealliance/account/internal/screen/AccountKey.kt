//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.account.internal.screen

import org.grovealliance.account.AccountKey
import org.grovealliance.account.AccountKeys
import org.grovealliance.account.AccountService
import org.grovealliance.account.AnyAccountKey
import org.grovealliance.account.userIdConfiguration
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.account.DataEntryComposable
import org.grovealliance.ui.account.StringDataEntry

/**
 * Retrieves the display name for the given [key] by accounting also for the user id [AccountService] configuration.
 */
internal fun AccountService.displayName(key: AnyAccountKey): StringResource {
    return if (key == AccountKeys.userId) configuration.userIdConfiguration.idType.label else key.name
}

/**
 * Retrieves the entry composable for the given [key] by accounting also for the user id [AccountService] configuration.
 */
@Suppress("UNCHECKED_CAST")
internal fun <V : Any> AccountService.requireEntryComposable(key: AccountKey<V>): DataEntryComposable<V> {
    val entry = requireNotNull(key.entry) { "No entry composable defined for key '${key.name}'" }
    return if (key == AccountKeys.userId && entry is StringDataEntry) {
        entry.copy(placeholder = configuration.userIdConfiguration.idType.label) as DataEntryComposable<V>
    } else {
        entry
    }
}
