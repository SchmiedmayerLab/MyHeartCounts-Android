//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account

import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountModifications
import org.grovealliance.account.AccountService
import org.grovealliance.account.AccountServiceConfiguration
import org.grovealliance.account.AuthProvider
import org.grovealliance.account.UserIdPasswordCredential

/**
 * An [AccountService] that records the account updates it receives and answers them with a
 * configured result. Every other operation succeeds without effect.
 */
class FakeAccountService : AccountService {
    private var updateResult: Result<Unit> = Result.success(Unit)
    private val _updates = mutableListOf<AccountModifications>()

    /**
     * The modifications passed to [updateAccountDetails], in call order.
     */
    val updates: List<AccountModifications> get() = _updates

    override val configuration: AccountServiceConfiguration
        get() = error("FakeAccountService has no configuration.")

    /**
     * Sets what [updateAccountDetails] returns.
     */
    fun setUpdateResult(result: Result<Unit>) {
        updateResult = result
    }

    override suspend fun updateAccountDetails(modifications: AccountModifications): Result<Unit> {
        _updates.add(modifications)
        return updateResult
    }

    override suspend fun signUp(signupDetails: AccountDetails): Result<Unit> = Result.success(Unit)

    override suspend fun logout(): Result<Unit> = Result.success(Unit)

    override suspend fun delete(): Result<Unit> = Result.success(Unit)

    override suspend fun login(credential: UserIdPasswordCredential): Result<Unit> = Result.success(Unit)

    override suspend fun resetPassword(userId: String): Result<Unit> = Result.success(Unit)

    override suspend fun sendEmailVerification(): Result<Unit> = Result.success(Unit)

    override suspend fun refresh(): Result<Unit> = Result.success(Unit)

    override suspend fun signIn(provider: AuthProvider): Result<Unit> = Result.success(Unit)
}
