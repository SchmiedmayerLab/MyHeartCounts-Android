//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.home

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import edu.stanford.myheartcounts.firebase.FirebaseSchema
import edu.stanford.myheartcounts.firebase.MHCFirestore
import edu.stanford.myheartcounts.firebase.mhcFirebaseLogger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.ui.StringResource
import java.time.Duration

/**
 * Surfaces the most recent nudge the backend sent the participant.
 *
 * The backend writes every nudge it sends into `users/{uid}/notificationHistory`, and the Home tab
 * shows the newest one back so a participant who dismissed or missed the notification still sees it.
 */
class FirebaseHomeSuggestionsSource(
    private val firestore: MHCFirestore,
    private val timeProvider: TimeProvider,
) : HomeSuggestionsSource {

    private val logger by mhcFirebaseLogger()

    override val nudge: Flow<DailyNudge?> = callbackFlow {

        val registration = firestore.notificationHistory()
            .orderBy(FirebaseSchema.NotificationHistory.ORIGINAL_TIMESTAMP, Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logger.e(error) { "Failed to observe the notification history." }
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.firstOrNull()?.let(::nudgeFrom))
            }
        awaitClose { registration.remove() }
    }

    /**
     * Always empty.
     *
     * Prompted actions are not modelled.
     */
    override val pendingActions: Flow<List<PromptedAction>> = flowOf(emptyList())

    /**
     * The nudge [document] describes, or `null` when it is too old to still be worth showing.
     *
     * A nudge is only surfaced on the day it was sent or the day after: past that it is history
     * rather than a prompt.
     */
    private fun nudgeFrom(document: DocumentSnapshot): DailyNudge? {
        val title = document.getString(FirebaseSchema.NotificationHistory.TITLE)
        val body = document.getString(FirebaseSchema.NotificationHistory.BODY)
        val sentAt = (document.get(FirebaseSchema.NotificationHistory.ORIGINAL_TIMESTAMP) as? Timestamp)?.toInstant()
        if (title == null || body == null || sentAt == null) return null

        val age = Duration.between(sentAt, timeProvider.nowInstant())
        if (age > MAX_AGE || age.isNegative) return null

        return DailyNudge(
            title = StringResource(text = title),
            message = StringResource(text = body),
        )
    }

    private companion object {
        /**
         * How long a nudge stays on the Home tab: one sent today or yesterday, expressed without
         * needing the participant's calendar.
         */
        val MAX_AGE: Duration = Duration.ofDays(2)
    }
}
