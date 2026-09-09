//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard

import androidx.health.connect.client.records.Record
import edu.stanford.myheartcounts.firebase.MHCFirebaseLoader
import edu.stanford.myheartcounts.notification.MHCPushTokenSynchronizer
import edu.stanford.myheartcounts.standard.health.HealthUploadWorker
import edu.stanford.myheartcounts.standard.health.MHCHealthDataHandler
import kotlinx.coroutines.launch
import org.grovealliance.account.Account
import org.grovealliance.account.observeIsSignedIn
import org.grovealliance.core.ApplicationModule
import org.grovealliance.core.Standard
import org.grovealliance.core.coroutines.Concurrency
import org.grovealliance.core.dependency
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.firebase.FirebaseAppConfiguration
import org.grovealliance.health.HealthConstraint
import org.grovealliance.health.RecordType
import org.grovealliance.study.StudyManager

/**
 * The single place the app's participant data flows through on its way to the backend.
 *
 * Grove modules that produce data — Health Connect records, questionnaire responses, consent — hand
 * it to the [Standard], which decides where it is stored. This is the Kotlin counterpart of iOS's
 * `MyHeartCountsStandard`, and it writes the same Firestore and Cloud Storage layout so that both
 * platforms feed one study dataset.
 *
 * It is also the app's [HealthConstraint]: Grove's health module reaches its constraint by casting
 * the configured standard, so health collection only works because this type implements it.
 *
 * Every backend operation is deferred until Firebase has been initialized, because the app talks to
 * a different Firebase project per region and which one is only known once the participant has
 * selected their country during onboarding.
 */
class MHCStandard : Standard, HealthConstraint {

    private val firebaseAppConfiguration by dependency<FirebaseAppConfiguration>()
    private val applicationModule by dependency<ApplicationModule>()
    private val account by dependency<Account>()
    private val concurrency by dependency<Concurrency>()
    private val pushTokens by dependency<MHCPushTokenSynchronizer>()
    private val environmentTracker by dependency<MHCEnvironmentTracker>()
    private val healthData by dependency<MHCHealthDataHandler>()
    private val studyManager by dependency<StudyManager>()

    private val ioScope by lazy { concurrency.ioCoroutineScope() }
    private val logger by groveLogger(tag = "MHCFirebase")

    override fun configure() {
        ioScope.launch {
            firebaseAppConfiguration.awaitConfigured()
            logger.i { "Firebase is available; the standard is ready to deliver data." }

            environmentTracker.start(scope = ioScope)

            var wasSignedIn = false
            account.observeIsSignedIn().collect { isSignedIn ->
                when {
                    isSignedIn -> onSignedIn()
                    // Only a real sign-out is worth tearing anything down for. The first emission is
                    // the state at launch, and for a launch with nobody signed in there is nothing
                    // running yet to stop.
                    wasSignedIn -> onSignedOut()
                    else -> Unit
                }
                wasSignedIn = isSignedIn
            }
        }
    }

    /**
     * Tears down the session while the participant is still authenticated.
     *
     * Called by [MHCAccountService][edu.stanford.myheartcounts.account.MHCAccountService] rather
     * than by a Grove event, because everything here needs credentials that a sign-out has already
     * taken away by the time Grove reports it. Ports iOS's `willLogOut` handling.
     */
    suspend fun willSignOut() {
        pushTokens.clear()
        healthData.clearPendingUploads()
        // Firestore refuses to clear its cache while its client is running, so the documents of the
        // participant signing out here can only go on the next process start.
        MHCFirebaseLoader.setShouldClearCacheOnNextLaunch(
            context = applicationModule.requireContext(),
            shouldClear = true,
        )
    }

    override suspend fun <T : Record> handleNewRecords(addedRecords: Set<T>, type: RecordType<out T>) {
        if (!isCollecting()) return
        healthData.handleNewRecords(records = addedRecords, type = type)
    }

    override suspend fun <T : Record> handleDeletedRecords(deletedRecordIds: Set<String>, type: RecordType<out T>) {
        if (!isCollecting()) return
        healthData.handleDeletedRecords(recordIds = deletedRecordIds, type = type)
    }

    override suspend fun <T : Record> onFullyResyncRequired(type: RecordType<out T>) {
        healthData.onFullyResyncRequired(type = type)
    }

    private suspend fun onSignedIn() {
        pushTokens.synchronize()
        // The tracker's own triggers only fire on a change, and the push it makes at start-up is
        // dropped when nobody is signed in yet — which is every first run.
        environmentTracker.pushAll()
        // An archive an earlier session was interrupted uploading belongs to whoever was signed in
        // then, so retrying it has to wait until somebody is signed in again.
        healthData.recoverInterruptedUploads()
        HealthUploadWorker.schedule(context = applicationModule.requireContext())
    }

    private fun onSignedOut() {
        logger.i { "The participant signed out." }
        HealthUploadWorker.cancel(context = applicationModule.requireContext())
    }

    /**
     * Whether health data should be collected at all.
     *
     * Matches iOS: only for a signed-in participant who is enrolled in the study. Collection can
     * keep delivering for a while after an unenrollment, and those samples have neither a place to
     * go nor consent behind them.
     */
    private suspend fun isCollecting(): Boolean =
        account.isSignedIn && studyManager.studyEnrollments().isNotEmpty()
}
