//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.grovealliance.account.Account
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountModifications

/**
 * The participant's choice to keep receiving activity nudges once the trial period has ended.
 */
interface PostTrialNudges {

    /**
     * Whether the signed-in participant opted in; `false` while nobody is signed in or no choice
     * has been made.
     */
    val isOptedIn: Flow<Boolean>

    /**
     * Records [optedIn] on the participant's account.
     */
    suspend fun setOptedIn(optedIn: Boolean): Result<Unit>
}

/**
 * [PostTrialNudges] stored on the participant's account.
 */
class PostTrialNudgesImpl(
    private val account: Account,
) : PostTrialNudges {

    override val isOptedIn: Flow<Boolean> = account.details
        .map { it?.postTrialNudgesOptIn ?: false }
        .distinctUntilChanged()

    override suspend fun setOptedIn(optedIn: Boolean): Result<Unit> {
        val modifiedDetails = AccountDetails()
        modifiedDetails[PostTrialNudgesOptInKey::class] = optedIn
        return AccountModifications(modifiedDetails = modifiedDetails)
            .mapCatching { account.service.updateAccountDetails(it).getOrThrow() }
    }
}
