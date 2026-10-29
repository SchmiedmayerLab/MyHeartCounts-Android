//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import edu.stanford.myheartcounts.account.StudyVariantKey
import edu.stanford.myheartcounts.account.studyVariant
import edu.stanford.myheartcounts.model.StudyVariant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.grovealliance.account.Account
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountModifications
import org.grovealliance.account.AccountService
import org.grovealliance.account.isAnonymousUser
import org.grovealliance.account.isIncomplete
import org.grovealliance.core.Module
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.study.StudyManager

/**
 * Keeps the [StudyVariant] consistent across the device, the account and the study content, on
 * [scope] from the moment it is configured.
 *
 * The variant stored on the signed-in account wins over the one selected on the device, whenever
 * either changes, so a participant keeps their variant across reinstalls. An account without a variant is given the
 * device's. Study content is resolved in the current variant's locale.
 */
class StudyVariantSynchronizer(
    private val account: Account,
    private val accountService: AccountService,
    private val variantProvider: StudyVariantProvider,
    private val studyManager: StudyManager,
    private val scope: CoroutineScope,
) : Module {

    private val logger by groveLogger()

    override fun configure() {
        scope.launch {
            variantProvider.variant.collect {
                studyManager.updatePreferredLocale(locale = variantProvider.contentLocale())
            }
        }
        scope.launch {
            combine(account.details, variantProvider.variant) { details, _ -> details }.collect { details ->
                if (details != null && !details.isAnonymousUser && !details.isIncomplete) {
                    synchronize(details = details)
                }
            }
        }
    }

    private suspend fun synchronize(details: AccountDetails) {
        val stored = details.studyVariant
        if (stored != null) {
            variantProvider.select(variant = stored)
            return
        }
        val modifiedDetails = AccountDetails()
        modifiedDetails[StudyVariantKey::class] = variantProvider.currentVariant
        AccountModifications(modifiedDetails = modifiedDetails)
            .mapCatching { accountService.updateAccountDetails(it).getOrThrow() }
            .onFailure { logger.e(it) { "Failed to record the study variant on the account" } }
    }
}
