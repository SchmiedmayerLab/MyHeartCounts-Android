//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.notification.MHCNotificationTracking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.grovealliance.account.firebase.FirebaseAuthProvider
import org.grovealliance.core.requireDependency
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Exercises the callable Cloud Functions and the notification-tracking write against the emulator
 * suite, including the anonymous sign-in the waitlist depends on.
 */
@RunWith(AndroidJUnit4::class)
class CloudFunctionsTest {

    private val cloudFunctions: MHCCloudFunctions get() = requireDependency()
    private val notificationTracking: MHCNotificationTracking get() = requireDependency()

    @Before
    fun initialize() = runTest {
        assumeTrue("Needs -Pmhc.firebaseEmulatorHost", FirebaseTestSession.isEmulatorConfigured)
        FirebaseTestSession.initializeFirebase()
    }

    @After
    fun signOut() = runTest {
        FirebaseTestSession.signOut()
    }

    @Test
    fun joinsTheWaitlistAsAnAnonymousParticipant() = runTest {
        // Exactly what onboarding does for a country the study has not launched in: the callable
        // needs an authenticated caller, and a participant there has no account yet.
        FirebaseTestSession.accountService.signIn(FirebaseAuthProvider.Anonymous).getOrThrow()

        val email = "waitlist-${UUID.randomUUID()}@myheartcounts.invalid"
        cloudFunctions.joinWaitlist(regionCode = "GB", email = email).getOrThrow()

        // Read back through the emulator's admin REST API rather than the client SDK: `waitlist` is
        // deliberately not client-readable under firestore.rules, and only the function's admin
        // credentials may touch it. Asserting through the client would be asserting that a rule the
        // study relies on is broken.
        val entry = emulatorDocument(path = "waitlist/GB_${email.lowercase()}")
        assertThat(entry).contains("\"stringValue\":\"GB\"")
        assertThat(entry).contains(email.lowercase())
    }

    /**
     * Fetches a document straight from the Firestore emulator, bypassing security rules.
     *
     * The emulator's REST surface is unauthenticated and rule-free by design, which is what makes it
     * usable to check a write only the backend is allowed to make.
     */
    private fun emulatorDocument(path: String): String {
        val host = requireNotNull(MHCFirebaseEmulator.host)
        val url = URL(
            "http://$host:${MHCFirebaseEmulator.FIRESTORE_PORT}" +
                "/v1/projects/myheart-counts-development/databases/(default)/documents/$path",
        )
        val connection = (url.openConnection() as HttpURLConnection).apply {
            // The emulator recognizes the literal bearer token "owner" as its own admin credential,
            // which is what lifts security rules for this read.
            setRequestProperty("Authorization", "Bearer owner")
        }
        return try {
            assertThat(connection.responseCode).isEqualTo(HttpURLConnection.HTTP_OK)
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    @Test
    fun marksTheAccountForStudyWithdrawalAndReenrollment() = runTest {
        FirebaseTestSession.signUpFreshParticipant()

        cloudFunctions.markAccountForStudyWithdrawal().getOrThrow()
        cloudFunctions.markAccountForStudyReenrollment().getOrThrow()
    }

    @Test
    fun recordsThatANotificationWasOpened() = runTest {
        FirebaseTestSession.signUpFreshParticipant()
        val notificationId = UUID.randomUUID().toString()

        notificationTracking.trackDidOpen(
            notificationId = notificationId,
            payload = mapOf("category" to "nudge-posttrial"),
        )

        val tracked = FirebaseTestSession.firestore.notificationTracking.get().await()
        val document = tracked.documents.single()
        assertThat(document.getString("notificationId")).isEqualTo(notificationId)
        assertThat(document.getString("event")).isEqualTo("opened")
        assertThat(document.getTimestamp("timestamp")).isNotNull()
        assertThat(document.getString("timeZone")).isNotEmpty()
    }
}
