//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.account

import org.grovealliance.foundation.KnowledgeSource

/**
 * A key for storing and retrieving configuration values in the [AccountServiceConfigurationStorage].
 *
 * @param T The type of the configuration value associated with this key.
 */
interface AccountServiceConfigurationKey<T : Any> : KnowledgeSource<AccountServiceConfigurationAnchor, T>

/**
 * Stores the value of this configuration key in the provided [AccountServiceConfigurationStorage].
 *
 * @param storage The storage where the configuration value should be stored.
 */
@Suppress("UNCHECKED_CAST")
fun <T : Any> AccountServiceConfigurationKey<T>.storeIn(storage: AccountServiceConfigurationStorage) {
    storage[this::class] = this as T
}
