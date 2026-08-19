//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.account

/**
 * Configuration entry describing how a specific [AccountKey] is required in a given context.
 *
 * This is typically used when defining validation or signup requirements.
 *
 * @param key the account key this configuration applies to
 * @param requirement how the key is expected to be provided (see [AccountKeyRequirement])
 */
data class AccountKeyConfiguration<T : Any>(
    val key: AccountKey<T>,
    val requirement: AccountKeyRequirement,
)
