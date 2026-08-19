//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.account

import java.util.concurrent.ConcurrentHashMap

/**
 * An in-memory [AccountStorageProvider] that caches the account values an [AccountService] cannot
 * store itself (its unsupported keys), keyed by `accountId`.
 *
 * Data is held only in memory and is not persisted across app restarts, but it does survive a
 * [disassociate] (logout) so that re-logging into the same account restores the externally stored
 * values — mirroring how a real provider (e.g. Firestore) persists them. [delete] clears the record.
 *
 * Intended for testing and demo purposes (e.g. with [InMemoryAccountService]).
 */
class InMemoryAccountStorageProvider : AccountStorageProvider {

    private val storage = ConcurrentHashMap<String, AccountDetails>()

    override suspend fun load(
        accountId: String,
        keys: Set<AnyAccountKey>,
    ): Result<AccountDetails> = Result.success(storage[accountId] ?: AccountDetails())

    override suspend fun store(
        accountId: String,
        modifications: AccountModifications,
    ): Result<Unit> {
        val details = storage.getOrPut(accountId) { AccountDetails() }
        details.addContents(modifications.modifiedDetails)
        details.removeAll(modifications.removedAccountKeys)
        return Result.success(Unit)
    }

    override suspend fun disassociate(accountId: String): Result<Unit> {
        return delete(accountId)
    }

    override suspend fun delete(accountId: String): Result<Unit> {
        storage.remove(accountId)
        return Result.success(Unit)
    }
}
