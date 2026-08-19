//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.account.internal

import org.grovealliance.account.Account
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountDetailsCodecConfig
import org.grovealliance.account.AccountService
import org.grovealliance.account.AccountStorageProvider
import org.grovealliance.account.AccountValueConfigurationBuilder
import org.grovealliance.account.ExternalAccountStorage
import org.grovealliance.account.internal.screen.AccountKeyEditSheetController
import org.grovealliance.account.internal.screen.AccountLoginViewModel
import org.grovealliance.account.internal.screen.AccountOverviewViewModel
import org.grovealliance.account.internal.screen.AccountSheetController
import org.grovealliance.account.internal.screen.AccountSignUpSheetController
import org.grovealliance.core.Configuration
import org.grovealliance.core.ConfigurationBuilder
import org.grovealliance.core.viewmodel.viewModel

/**
 * Registers the necessary account related modules for Grove based on the provided configuration.
 */
internal class AccountModulesBuilder internal constructor(
    private val service: AccountService,
    private val storageProvider: AccountStorageProvider?,
    private val initialDetails: AccountDetails?,
    private val codecConfig: AccountDetailsCodecConfig,
    valueConfigurationBuilder: AccountValueConfigurationBuilder.() -> Unit,
) {
    private val keysBuilder = AccountValueConfigurationBuilder().apply(valueConfigurationBuilder)

    fun register(configurationBuilder: ConfigurationBuilder) = with(configurationBuilder) {
        storageProvider?.let { module { it } }
        module { ExternalAccountStorage(storageProvider = storageProvider) }
        singleton { codecConfig }
        module { service }
        module<Account> {
            AccountImpl(
                service = service,
                configuration = keysBuilder.buildConfiguration(),
                initialDetails = initialDetails,
            )
        }

        include(accountScreensConfiguration())
    }

    private fun accountScreensConfiguration() = Configuration {
        factory {
            AccountSheetController(
                account = dependency(),
                accountService = dependency(),
                signUpSheetController = dependency(),
                editKeySheetController = dependency(),
            )
        }

        factory {
            AccountSignUpSheetController(
                account = dependency(),
                accountService = dependency(),
            )
        }

        factory {
            AccountKeyEditSheetController(
                accountService = dependency(),
            )
        }

        viewModel {
            AccountLoginViewModel(
                accountService = dependency(),
                accountSheetController = dependency(),
            )
        }

        viewModel {
            AccountOverviewViewModel(
                account = dependency(),
                accountSheetController = dependency(),
            )
        }
    }
}
