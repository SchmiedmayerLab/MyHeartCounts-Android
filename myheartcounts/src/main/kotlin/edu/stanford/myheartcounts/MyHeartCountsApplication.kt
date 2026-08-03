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
import edu.stanford.myheartcounts.study.MHCStudyBundleProvider
import edu.stanford.myheartcounts.study.STUDY_BUNDLE_ASSET_PATH
import edu.stanford.spezi.account.Account
import edu.stanford.spezi.account.name
import edu.stanford.spezi.consent.ConsentDocument
import edu.stanford.spezi.consent.SignatureMetadata
import edu.stanford.spezi.consent.consent
import edu.stanford.spezi.core.Configuration
import edu.stanford.spezi.core.SpeziApplication
import edu.stanford.spezi.core.dependency
import edu.stanford.spezi.core.logging.SpeziLogger
import edu.stanford.spezi.scheduler.SchedulerNotificationsConfiguration
import edu.stanford.spezi.scheduler.scheduler
import edu.stanford.spezi.study.studyManager
import java.util.Locale

/**
 * The consent document used before the study bundle is available, which carries the same text as the
 * bundle's default localization.
 */
private const val FALLBACK_CONSENT_ASSET = "$STUDY_BUNDLE_ASSET_PATH/consent/Consent+en-US.md"

/**
 * The application entry point. Declares the app's dependency graph and consent document, and enables
 * logging in debug builds.
 */
class MyHeartCountsApplication : Application(), SpeziApplication {

    override val configuration: Configuration = Configuration {
        account()
        appConfigurations()
        appViewModels()

        scheduler(notifications = SchedulerNotificationsConfiguration.DEFAULT)
        studyManager()

        consent {
            document {
                val studyBundleProvider by dependency<MHCStudyBundleProvider>()
                studyBundleProvider.get().getOrNull()
                    ?.consentText(locale = Locale.getDefault())
                    ?.let { ConsentDocument.Text(text = it) }
                    ?: ConsentDocument.Asset(filename = FALLBACK_CONSENT_ASSET)
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
