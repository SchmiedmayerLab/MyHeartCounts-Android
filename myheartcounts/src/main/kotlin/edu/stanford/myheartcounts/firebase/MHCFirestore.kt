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
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import org.grovealliance.account.Account
import org.grovealliance.account.accountId
import org.grovealliance.firebase.FirebaseAppConfiguration

/**
 * Resolves the participant's Firestore and Cloud Storage locations, waiting for Firebase
 * initialization internally before returning a reference or account identifier.
 */
@Suppress("detekt:TooManyFunctions")
interface MHCFirestore {

    /**
     * The identifier of the signed-in participant.
     *
     * @throws MHCFirebaseError.NotSignedIn if nobody is signed in.
     */
    suspend fun accountId(): String

    /**
     * The collection holding one document per participant.
     */
    suspend fun usersCollection(): CollectionReference

    /**
     * The signed-in participant's document.
     */
    suspend fun userDocument(): DocumentReference

    /**
     * The participant's questionnaire responses, stored as FHIR `QuestionnaireResponse` documents.
     */
    suspend fun questionnaireResponses(): CollectionReference

    /**
     * Records of the participant interacting with a notification.
     */
    suspend fun notificationTracking(): CollectionReference

    /**
     * The notifications the backend has sent the participant. Server-written and read-only here.
     */
    suspend fun notificationHistory(): CollectionReference

    /**
     * The collection participants' feedback is written to, one document per message.
     */
    suspend fun feedback(): CollectionReference

    /**
     * The Cloud Storage folder of the participant identified by [accountId].
     */
    suspend fun userStorageFolder(accountId: String): StorageReference

    /**
     * The Cloud Storage folder holding the signed-in participant's signed consent forms.
     */
    suspend fun consentForms(): StorageReference

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
    suspend fun healthObservations(sampleTypeIdentifier: String): CollectionReference

    /**
     * The participant's monthly statistics documents of one metric, each named after its month as
     * `yyyy-MM`.
     */
    suspend fun healthStatsMonths(metricId: String): CollectionReference
}

/**
 * Default [MHCFirestore] implementation, resolving locations in the default Firebase app.
 */
class MHCFirestoreImpl(
    private val account: Account,
    private val firebaseAppConfiguration: FirebaseAppConfiguration,
) : MHCFirestore {

    override suspend fun accountId(): String {
        firebaseAppConfiguration.awaitConfigured()
        return account.details.value?.accountId ?: throw MHCFirebaseError.NotSignedIn()
    }

    override suspend fun usersCollection(): CollectionReference {
        firebaseAppConfiguration.awaitConfigured()
        return FirebaseFirestore.getInstance().collection(FirebaseSchema.Collection.USERS)
    }

    override suspend fun userDocument(): DocumentReference = usersCollection().document(accountId())

    override suspend fun questionnaireResponses(): CollectionReference =
        userDocument().collection(FirebaseSchema.Collection.QUESTIONNAIRE_RESPONSES)

    override suspend fun notificationTracking(): CollectionReference =
        userDocument().collection(FirebaseSchema.Collection.NOTIFICATION_TRACKING)

    override suspend fun notificationHistory(): CollectionReference =
        userDocument().collection(FirebaseSchema.Collection.NOTIFICATION_HISTORY)

    override suspend fun feedback(): CollectionReference {
        firebaseAppConfiguration.awaitConfigured()
        return FirebaseFirestore.getInstance().collection(FirebaseSchema.Collection.FEEDBACK)
    }

    override suspend fun userStorageFolder(accountId: String): StorageReference {
        firebaseAppConfiguration.awaitConfigured()
        return FirebaseStorage.getInstance().reference
            .child(FirebaseSchema.Storage.USERS)
            .child(accountId)
    }

    override suspend fun consentForms(): StorageReference =
        userStorageFolder(accountId = accountId()).child(FirebaseSchema.Storage.CONSENT)

    override suspend fun healthObservations(sampleTypeIdentifier: String): CollectionReference =
        userDocument().collection("${FirebaseSchema.Collection.HEALTH_OBSERVATIONS_PREFIX}$sampleTypeIdentifier")

    override suspend fun healthStatsMonths(metricId: String): CollectionReference =
        userDocument().collection(FirebaseSchema.Collection.STATS)
            .document(metricId)
            .collection(FirebaseSchema.Collection.STATS_MONTHS)
}
