//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account

import edu.stanford.myheartcounts.firebase.MHCCloudFunctions
import org.grovealliance.account.AccountService

/**
 * Withdraws the signed-in participant from the study.
 */
interface StudyWithdrawal {

    /**
     * Marks the account as withdrawn and then signs the participant out.
     *
     * The account stays signed in when it could not be marked, so a failed withdrawal can be retried.
     * The account is kept rather than deleted; the participant can re-enroll later.
     */
    suspend fun withdraw(): Result<Unit>
}

/**
 * [StudyWithdrawal] that marks the account through the backend before signing out.
 */
class StudyWithdrawalImpl(
    private val cloudFunctions: MHCCloudFunctions,
    private val accountService: AccountService,
) : StudyWithdrawal {

    override suspend fun withdraw(): Result<Unit> =
        cloudFunctions.markAccountForStudyWithdrawal()
            .mapCatching { accountService.logout().getOrThrow() }
}
