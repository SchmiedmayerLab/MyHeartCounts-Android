//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts

import android.app.Application
import android.content.Context
import edu.stanford.myheartcounts.account.account
import edu.stanford.myheartcounts.di.appConfigurations
import edu.stanford.myheartcounts.di.appViewModels
import edu.stanford.myheartcounts.firebase.MHCFirebaseEmulator
import edu.stanford.myheartcounts.firebase.MHCFirebaseLoader
import edu.stanford.myheartcounts.firebase.MHCFirestoreSetup
import edu.stanford.myheartcounts.standard.MHCStandard
import edu.stanford.myheartcounts.standard.consent.MHCConsentDocumentProvider
import edu.stanford.myheartcounts.standard.health.mhcHealth
import org.grovealliance.account.Account
import org.grovealliance.account.name
import org.grovealliance.consent.ConsentDocument
import org.grovealliance.consent.SignatureMetadata
import org.grovealliance.consent.consent
import org.grovealliance.core.Configuration
import org.grovealliance.core.GroveApplication
import org.grovealliance.core.dependency
import org.grovealliance.core.logging.GroveLogger
import org.grovealliance.firebase.firebaseApp
import org.grovealliance.scheduler.SchedulerNotificationsConfiguration
import org.grovealliance.scheduler.scheduler
import org.grovealliance.study.studyManager
import java.util.Locale
import androidx.work.Configuration as WorkConfiguration

/**
 * The application entry point. Declares the app's dependency graph and consent document, and enables
 * logging in debug builds.
 *
 * It is also WorkManager's [WorkConfiguration.Provider]. Grove bootstraps from a content provider
 * with a higher `initOrder` than WorkManager's own initializer, so a module configured by Grove that
 * touches `WorkManager.getInstance` would otherwise find it uninitialized and throw. Supplying the
 * configuration here lets WorkManager initialize on demand, whenever it is first asked for.
 */
class MyHeartCountsApplication : Application(), GroveApplication, WorkConfiguration.Provider {

    override val workManagerConfiguration: WorkConfiguration = WorkConfiguration.Builder().build()

    /**
     * Deliberately lazy: Grove reads this from its content provider, after the base context is
     * attached, whereas an eager property initializer would run while this [Application] is still
     * being constructed. Resolving the participant's Firebase project needs a context.
     */
    override val configuration: Configuration by lazy {
        Configuration(standard = MHCStandard()) {
            // Registered before every Firebase-backed module, so they can await initialization. On a
            // first launch no region has been picked yet, the options below are null, and Firebase
            // stays uninitialized until MHCFirebaseRegionInitializer supplies them mid-onboarding.
            firebaseApp(
                options = MHCFirebaseLoader.persistedRegionOptions(
                    context = this@MyHeartCountsApplication,
                ),
                onInitialized = {
                    MHCFirebaseEmulator.redirectIfConfigured()
                    MHCFirestoreSetup.apply(context = this@MyHeartCountsApplication)
                },
            )

            account()
            appConfigurations()
            appViewModels()
            mhcHealth()

            scheduler(notifications = SchedulerNotificationsConfiguration.DEFAULT)
            studyManager(
                preferredLocale = MHCFirebaseLoader.persistedRegionLocale(
                    context = this@MyHeartCountsApplication,
                ) ?: Locale.getDefault(),
            )

            consent {
                document {
                    // The same source the signed PDF is rendered from, so the document a
                    // participant reads and the record kept of it cannot diverge.
                    val documentProvider by dependency<MHCConsentDocumentProvider>()
                    ConsentDocument.Text(text = documentProvider.text())
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
    }

    /**
     * Enables logging here rather than in `onCreate`, which runs after Grove has already configured
     * every module from its content provider — the start-up that most needs to be readable.
     */
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

        GroveLogger.setLoggingEnabled(enabled = BuildConfig.DEBUG)
    }
}
