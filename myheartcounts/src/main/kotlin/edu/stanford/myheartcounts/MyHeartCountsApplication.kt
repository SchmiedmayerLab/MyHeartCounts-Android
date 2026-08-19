//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts

import android.app.Application
import edu.stanford.myheartcounts.account.account
import edu.stanford.myheartcounts.di.appConfigurations
import edu.stanford.myheartcounts.di.appViewModels
import org.grovealliance.account.Account
import org.grovealliance.account.name
import org.grovealliance.consent.ConsentDocument
import org.grovealliance.consent.SignatureMetadata
import org.grovealliance.consent.consent
import org.grovealliance.core.Configuration
import org.grovealliance.core.GroveApplication
import org.grovealliance.core.dependency
import org.grovealliance.core.logging.GroveLogger

/**
 * The application entry point. Declares the app's dependency graph and consent document, and enables
 * logging in debug builds.
 */
class MyHeartCountsApplication : Application(), GroveApplication {

    override val configuration: Configuration = Configuration {
        account()
        appConfigurations()
        appViewModels()

        consent {
            document {
                ConsentDocument.Asset(filename = "Consent+en-US.md")
            }
            initialSignatureMetadata {
                val account by dependency<Account>()
                val name = account.details.value?.name
                SignatureMetadata(
                    givenName = name?.givenName.orEmpty(),
                    familyName = name?.familyName.orEmpty(),
                    strokes = emptyList(),
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        GroveLogger.setLoggingEnabled(enabled = BuildConfig.DEBUG)
    }
}
