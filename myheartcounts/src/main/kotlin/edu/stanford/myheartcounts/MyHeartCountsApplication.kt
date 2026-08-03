//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts

import android.app.Application
import edu.stanford.myheartcounts.account.account
import edu.stanford.myheartcounts.di.appConfigurations
import edu.stanford.myheartcounts.di.appViewModels
import edu.stanford.spezi.account.Account
import edu.stanford.spezi.account.name
import edu.stanford.spezi.consent.ConsentDocument
import edu.stanford.spezi.consent.SignatureMetadata
import edu.stanford.spezi.consent.consent
import edu.stanford.spezi.core.Configuration
import edu.stanford.spezi.core.SpeziApplication
import edu.stanford.spezi.core.dependency
import edu.stanford.spezi.core.logging.SpeziLogger

/**
 * The consent document within the study bundle shipped in the app's assets.
 */
private const val CONSENT_ASSET = "mhcStudyBundle.spezistudybundle/consent/Consent+en-US.md"

/**
 * The application entry point. Declares the app's dependency graph and consent document, and enables
 * logging in debug builds.
 */
class MyHeartCountsApplication : Application(), SpeziApplication {

    override val configuration: Configuration = Configuration {
        account()
        appConfigurations()
        appViewModels()

        consent {
            document {
                // TODO: Resolve the consent text from the study bundle for the device locale,
                //  keeping this asset as the fallback.
                ConsentDocument.Asset(filename = CONSENT_ASSET)
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

        SpeziLogger.setLoggingEnabled(enabled = BuildConfig.DEBUG)
    }
}
