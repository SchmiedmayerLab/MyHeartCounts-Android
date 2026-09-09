//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import org.grovealliance.core.Module
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.firebase.FirebaseAppConfiguration

/**
 * The callable Cloud Functions the app invokes.
 *
 * These are the same three iOS calls, against the same `MyHeartCounts-Firebase` deployment. Note
 * that the backend also exports `markAccountForDeletion`, which neither platform calls: account
 * deletion goes through `AccountService.delete()` instead.
 */
interface MHCCloudFunctions {

    /**
     * Adds [email] to the launch waitlist for [regionCode].
     *
     * The backend keys the waitlist entry on region and email, so calling this twice for the same
     * pair updates the existing entry rather than adding a second one. It requires a signed-in
     * participant, which for a country the study has not launched in means an anonymous sign-up.
     *
     * @param regionCode An ISO 3166-1 alpha-2 country code.
     * @param email The address to notify when the study launches there.
     */
    suspend fun joinWaitlist(regionCode: String, email: String): Result<Unit>

    /**
     * Marks the signed-in participant as withdrawn from the study.
     *
     * The caller logs out afterwards, matching iOS.
     */
    suspend fun markAccountForStudyWithdrawal(): Result<Unit>

    /**
     * Reverses a withdrawal, re-enrolling the signed-in participant into the study.
     */
    suspend fun markAccountForStudyReenrollment(): Result<Unit>
}

/**
 * Default [MHCCloudFunctions] implementation, calling into the Firebase project the participant's
 * region resolved to.
 */
class MHCCloudFunctionsImpl(
    private val firebaseAppConfiguration: FirebaseAppConfiguration,
) : MHCCloudFunctions, Module {

    private val logger by groveLogger(tag = "MHCFirebase")

    override suspend fun joinWaitlist(regionCode: String, email: String): Result<Unit> =
        call(
            name = "joinWaitlist",
            data = mapOf("region" to regionCode, "email" to email),
        )

    override suspend fun markAccountForStudyWithdrawal(): Result<Unit> =
        call(name = "markAccountForStudyWithdrawal", data = emptyMap<String, Any>())

    override suspend fun markAccountForStudyReenrollment(): Result<Unit> =
        call(name = "markAccountForStudyReenrollment", data = emptyMap<String, Any>())

    /**
     * Invokes the callable [name] with [data], waiting for Firebase to be initialized first.
     *
     * Awaiting rather than failing fast is deliberate: the waitlist call happens during onboarding,
     * moments after region selection kicked off initialization.
     */
    private suspend fun call(name: String, data: Any): Result<Unit> = runCatching {
        firebaseAppConfiguration.awaitConfigured()
        FirebaseFunctions.getInstance().getHttpsCallable(name).call(data).await()
        Unit
    }.onFailure { throwable ->
        logger.e(throwable) { "Cloud function '$name' failed." }
    }
}
