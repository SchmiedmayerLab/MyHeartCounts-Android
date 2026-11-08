//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.comorbidities

import edu.stanford.myheartcounts.account.ComorbiditiesKey
import edu.stanford.myheartcounts.account.comorbidities
import edu.stanford.myheartcounts.model.demographics.Comorbidities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.grovealliance.account.Account
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountModifications
import org.grovealliance.core.logging.groveLogger

/**
 * The signed-in participant's comorbidities as stored on their account.
 */
interface ComorbiditiesStorage {

    /**
     * The stored comorbidities, or `null` while nobody is signed in or none were reported.
     */
    val currentComorbidities: Comorbidities?

    /**
     * Starts storing [comorbidities] on the participant's account, without waiting for it; a failure
     * is logged.
     */
    fun storeSoon(comorbidities: Comorbidities)
}

/**
 * [ComorbiditiesStorage] on the participant's [account], storing in [scope] so a store outlives the
 * screen that asked for it.
 */
class ComorbiditiesStorageImpl(
    private val account: Account,
    private val scope: CoroutineScope,
) : ComorbiditiesStorage {

    private val logger by groveLogger()

    override val currentComorbidities: Comorbidities?
        get() = account.details.value?.comorbidities

    override fun storeSoon(comorbidities: Comorbidities) {
        scope.launch {
            val modifiedDetails = AccountDetails()
            modifiedDetails[ComorbiditiesKey::class] = comorbidities
            AccountModifications(modifiedDetails = modifiedDetails)
                .mapCatching { account.service.updateAccountDetails(it).getOrThrow() }
                .onFailure { throwable -> logger.e(throwable) { "Storing the comorbidities failed." } }
        }
    }
}
