//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.feedback

import com.google.firebase.Timestamp
import edu.stanford.myheartcounts.firebase.FirebaseSchema
import edu.stanford.myheartcounts.firebase.MHCFirestore
import edu.stanford.myheartcounts.standard.MHCAppRevision
import kotlinx.coroutines.tasks.await
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.core.time.TimeProvider
import java.util.UUID

/**
 * Sends the participant's feedback about the app to the study team.
 */
interface MHCFeedback {

    /**
     * Sends [message], together with the participant's account, the time, the app version and the
     * device, so the team can follow up.
     */
    suspend fun submit(message: String): Result<Unit>
}

/**
 * [MHCFeedback] storing each message as a document in the feedback collection.
 */
class MHCFeedbackImpl(
    private val firestore: MHCFirestore,
    private val appRevision: MHCAppRevision,
    private val timeProvider: TimeProvider,
) : MHCFeedback {

    override suspend fun submit(message: String): Result<Unit> = runCatchingAsync {
        val now = timeProvider.nowInstant()
        val document = mapOf(
            FirebaseSchema.Feedback.VERSION to DOCUMENT_VERSION,
            FirebaseSchema.Feedback.ACCOUNT_ID to firestore.accountId(),
            FirebaseSchema.Feedback.MESSAGE to message,
            FirebaseSchema.Feedback.DATE to Timestamp(now.epochSecond, now.nano),
            FirebaseSchema.Feedback.TIME_ZONE to timeProvider.currentZone().id,
            FirebaseSchema.Feedback.APP_VERSION to appRevision.version,
            FirebaseSchema.Feedback.APP_BUILD_NUMBER to appRevision.build,
            FirebaseSchema.Feedback.DEVICE_INFO to mapOf(
                FirebaseSchema.Feedback.OS_VERSION to appRevision.osVersion,
                FirebaseSchema.Feedback.MODEL to appRevision.deviceModel,
                FirebaseSchema.Feedback.TYPE to appRevision.deviceType,
            ),
        )
        firestore.feedback()
            .document(UUID.randomUUID().toString().uppercase())
            .set(document)
            .await()
        Unit
    }

    private companion object {
        /**
         * The version of the feedback document's structure.
         */
        const val DOCUMENT_VERSION = 1
    }
}
