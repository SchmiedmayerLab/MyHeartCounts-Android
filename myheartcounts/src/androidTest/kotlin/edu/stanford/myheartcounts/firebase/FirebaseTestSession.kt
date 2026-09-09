//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

import com.google.firebase.firestore.DocumentSnapshot
import edu.stanford.myheartcounts.model.Country
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.grovealliance.account.Account
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.accountId
import org.grovealliance.account.AccountService
import org.grovealliance.account.DateOfBirthKey
import org.grovealliance.account.UserIdKey
import org.grovealliance.account.NameKey
import org.grovealliance.account.PasswordKey
import org.grovealliance.account.PersonName
import org.grovealliance.core.requireDependency
import java.time.Instant
import java.util.UUID

/**
 * Signs a throwaway participant in against the local Firebase emulator suite, so a test can exercise
 * the app's real Firebase paths.
 *
 * These tests only mean anything when the build was given an emulator host — without one they would
 * be writing into `myheart-counts-development`. [isEmulatorConfigured] is what every test checks
 * before doing anything, so a run without the flag reports as skipped rather than as passing.
 */
object FirebaseTestSession {

    /**
     * Whether this build targets a local emulator suite. See `MHCFirebaseEmulator`.
     */
    val isEmulatorConfigured: Boolean get() = MHCFirebaseEmulator.host != null

    val account: Account get() = requireDependency()
    val accountService: AccountService get() = requireDependency()
    val firestore: MHCFirestore get() = requireDependency()

    /**
     * Brings Firebase up for the United States region, as onboarding does once a participant picks
     * their country, and waits until it is usable.
     */
    suspend fun initializeFirebase() {
        requireDependency<MHCFirebaseRegionInitializer>()
            .initialize(country = Country(code = "US", name = "United States"))
        withTimeout(READY_TIMEOUT_MILLIS) { firestore.awaitReady() }
    }

    /**
     * Signs up a participant nobody else will collide with, and waits until the account reports them
     * as signed in.
     *
     * @return The email address the participant was created with.
     */
    suspend fun signUpFreshParticipant(): String {
        // Anyone left over from an earlier test has to be gone before the new participant is waited
        // for, or the wait below is satisfied instantly by the previous account id.
        signOut()

        val email = "test-${UUID.randomUUID()}@myheartcounts.invalid"
        val details = AccountDetails()
        // The credential Grove signs up with is `userId`, not `email` — `email` is *computed* from
        // it when the user-id type is an email address, so setting email alone leaves the sign-up
        // with no identifier at all.
        details[UserIdKey::class] = email
        details[PasswordKey::class] = PASSWORD
        details[NameKey::class] = PersonName(givenName = "Test", familyName = "Participant")
        details[DateOfBirthKey::class] = Instant.parse("1990-06-15T00:00:00Z")

        accountService.signUp(signupDetails = details).getOrThrow()
        withTimeout(READY_TIMEOUT_MILLIS) {
            account.details.first { it?.accountId != null }
        }
        return email
    }

    /**
     * The participant's raw Firestore document — the wire format both platforms share, rather than
     * the app's decoded view of it.
     *
     * Asserting against this is deliberate: a codec bug that encodes and decodes consistently would
     * round-trip perfectly through the app and still be invisible to iOS.
     */
    suspend fun rawUserDocument(): DocumentSnapshot = firestore.userDocument.get().await()

    /**
     * Signs the participant out and waits until the account reflects it.
     *
     * Waiting matters between tests: `accountId` is what every Firestore and Storage path is built
     * from, so a test that starts while the previous participant is still current would silently
     * write into their document instead of its own.
     */
    suspend fun signOut() {
        runCatching {
            accountService.logout()
            withTimeout(READY_TIMEOUT_MILLIS) { account.details.first { it == null } }
        }
    }

    private const val PASSWORD = "TestPassword123!"
    private const val READY_TIMEOUT_MILLIS = 30_000L
}
