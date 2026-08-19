//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.account.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.grovealliance.account.Account
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountKeys
import org.grovealliance.account.AccountService
import org.grovealliance.account.AccountValueConfiguration
import org.grovealliance.account.PasswordKey
import org.grovealliance.account.accountLogger
import org.grovealliance.account.accountServiceConfiguration

internal class AccountImpl(
    override val service: AccountService,
    override val configuration: AccountValueConfiguration,
    initialDetails: AccountDetails?,
) : Account {
    private val logger by accountLogger()

    private val _details = MutableStateFlow(initialDetails)
    override val details = _details.asStateFlow()

    override fun supplyUserDetails(details: AccountDetails) {
        require(details.contains(AccountKeys.accountId)) { "AccountDetails must contain accountId." }
        details[PasswordKey::class] = null
        details.accountServiceConfiguration = service.configuration
        _details.update { details.copy() }
    }

    override fun removeUserDetails() {
        _details.update { null }
    }

    override fun configure() {
        if (configuration[AccountKeys.userId::class] == null) {
            logger.w {
                """
                Your AccountConfiguration doesn't have the user id configured.
                A primary, user-visible identifier is recommended with most GroveAccount components for
                an optimal user experience. Ignore this warning if you know what you are doing.
                """
            }
        }
    }
}
