//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.grovealliance.account.isVerified
import org.grovealliance.core.requireDependency
import org.json.JSONObject
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL

/**
 * Verifies a participant's email address against the Firebase Auth emulator: the account reports the
 * address as unverified after sign-up, and as verified once the verification link was opened and the
 * account reloaded.
 */
@RunWith(AndroidJUnit4::class)
class EmailVerificationTest {

    private val host: String get() = requireNotNull(requireDependency<MHCFirebaseEmulator>().host)
    private lateinit var email: String

    @Before
    fun signIn() = runTest {
        assumeTrue("Needs -Pmhc.firebaseEmulatorHost", FirebaseTestSession.isEmulatorConfigured)
        FirebaseTestSession.initializeFirebase()
        email = FirebaseTestSession.signUpFreshParticipant()
    }

    @After
    fun signOut() = runTest {
        FirebaseTestSession.signOut()
    }

    @Test
    fun reportsTheEmailAsVerifiedOnceTheLinkWasOpenedAndTheAccountReloaded() = runTest {
        // given
        assertThat(FirebaseTestSession.account.details.value?.isVerified).isFalse()
        FirebaseTestSession.accountService.sendEmailVerification().getOrThrow()

        // when
        openVerificationLink()
        FirebaseTestSession.accountService.refresh().getOrThrow()

        // then
        assertThat(FirebaseTestSession.account.details.value?.isVerified).isTrue()
    }

    @Test
    fun keepsTheEmailUnverifiedWhenReloadedBeforeTheLinkWasOpened() = runTest {
        // given
        FirebaseTestSession.accountService.sendEmailVerification().getOrThrow()

        // when
        FirebaseTestSession.accountService.refresh().getOrThrow()

        // then
        assertThat(FirebaseTestSession.account.details.value?.isVerified).isFalse()
    }

    /**
     * Applies the most recent verification code the Auth emulator issued for [email], as opening
     * the link in the email would.
     */
    private suspend fun openVerificationLink() = withContext(Dispatchers.IO) {
        val options = FirebaseApp.getInstance().options
        val codes = JSONObject(get(url = "http://$host:$AUTH_PORT/emulator/v1/projects/${options.projectId}/oobCodes"))
            .getJSONArray("oobCodes")
        val code = (0 until codes.length())
            .map { codes.getJSONObject(it) }
            .last { it.getString("email") == email && it.getString("requestType") == "VERIFY_EMAIL" }
            .getString("oobCode")
        get(url = "http://$host:$AUTH_PORT/emulator/action?mode=verifyEmail&oobCode=$code&apiKey=${options.apiKey}")
    }

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            check(connection.responseCode in 200..299) { "GET $url failed with ${connection.responseCode}." }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val AUTH_PORT = 9099
    }
}
