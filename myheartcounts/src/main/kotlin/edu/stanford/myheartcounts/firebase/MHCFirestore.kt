//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import edu.stanford.myheartcounts.account.USERS_COLLECTION
import org.grovealliance.account.Account
import org.grovealliance.account.accountId
import org.grovealliance.core.Module
import org.grovealliance.core.dependency
import org.grovealliance.firebase.FirebaseAppConfiguration

/**
 * Resolves the Firestore locations the app reads and writes, all of them below the signed-in
 * participant's document.
 *
 * The layout is shared with My Heart Counts for iOS and the `MyHeartCounts-Firebase` backend, so
 * the paths here are a contract rather than an implementation detail. This mirrors iOS's
 * `FirebaseConfiguration`.
 *
 * Every accessor needs a signed-in participant and an initialized Firebase app, so a caller has to
 * be behind [awaitReady] — which is why the gate lives here rather than being something each caller
 * has to remember to hold separately.
 */
class MHCFirestore : Module {

    private val account by dependency<Account>()
    private val firebaseAppConfiguration by dependency<FirebaseAppConfiguration>()

    /**
     * Suspends until Firebase is initialized and the accessors below are safe to use.
     *
     * On a first launch that is only once the participant has picked their region during onboarding,
     * because that is what decides which Firebase project the app talks to.
     */
    suspend fun awaitReady() {
        firebaseAppConfiguration.awaitConfigured()
    }

    /**
     * The identifier of the signed-in participant.
     *
     * @throws MHCFirebaseError.NotSignedIn if nobody is signed in.
     */
    val accountId: String
        get() = account.details.value?.accountId ?: throw MHCFirebaseError.NotSignedIn()

    /**
     * The collection holding one document per participant.
     */
    val usersCollection: CollectionReference
        get() = FirebaseFirestore.getInstance().collection(USERS_COLLECTION)

    /**
     * The signed-in participant's document.
     */
    val userDocument: DocumentReference
        get() = usersCollection.document(accountId)

    /**
     * The participant's questionnaire responses, stored as FHIR `QuestionnaireResponse` documents.
     */
    val questionnaireResponses: CollectionReference
        get() = userDocument.collection(QUESTIONNAIRE_RESPONSES_COLLECTION)

    /**
     * The queue of health samples the participant deleted, which the backend replays against the
     * collections the samples were uploaded to.
     */
    val pendingHealthSampleDeletions: CollectionReference
        get() = userDocument.collection(PENDING_HEALTH_SAMPLE_DELETIONS_COLLECTION)

    /**
     * Records of the participant interacting with a notification.
     */
    val notificationTracking: CollectionReference
        get() = userDocument.collection(NOTIFICATION_TRACKING_COLLECTION)

    /**
     * The notifications the backend has sent the participant. Server-written and read-only here.
     */
    val notificationHistory: CollectionReference
        get() = userDocument.collection(NOTIFICATION_HISTORY_COLLECTION)

    /**
     * The collection holding the FHIR `Observation`s for one health sample type.
     *
     * The collection name carries the sample-type identifier, so that samples of the same kind from
     * either platform land together. The backend only accepts identifiers matching
     * `HealthObservations_[A-Za-z][A-Za-z0-9]*`.
     *
     * @param sampleTypeIdentifier A HealthKit-style sample type identifier, such as
     * `HKQuantityTypeIdentifierStepCount`.
     */
    fun healthObservations(sampleTypeIdentifier: String): CollectionReference =
        userDocument.collection("$HEALTH_OBSERVATIONS_PREFIX$sampleTypeIdentifier")

    private companion object {
        const val QUESTIONNAIRE_RESPONSES_COLLECTION = "questionnaireResponses"
        const val PENDING_HEALTH_SAMPLE_DELETIONS_COLLECTION = "pendingHealthSampleDeletions"
        const val NOTIFICATION_TRACKING_COLLECTION = "notificationTracking"
        const val NOTIFICATION_HISTORY_COLLECTION = "notificationHistory"
        const val HEALTH_OBSERVATIONS_PREFIX = "HealthObservations_"
    }
}
