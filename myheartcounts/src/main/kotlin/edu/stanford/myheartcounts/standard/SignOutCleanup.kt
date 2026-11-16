//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard

import edu.stanford.myheartcounts.firebase.MHCFirebaseLoader
import edu.stanford.myheartcounts.firebase.mhcFirebaseLogger
import edu.stanford.myheartcounts.onboarding.OnboardingCompletion
import edu.stanford.myheartcounts.standard.health.MHCHealthDataHandler
import edu.stanford.myheartcounts.study.StudyActivation
import edu.stanford.myheartcounts.study.StudyVariantProvider
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.study.StudyManager

/**
 * Removes what a participant leaves behind when signing out, so the next participant to sign in
 * inherits none of it.
 */
interface SignOutCleanup {

    /**
     * Removes what is cleaned up while the participant is still signed in.
     */
    suspend fun beforeSignOut()

    /**
     * Removes what is left once the participant is signed out, including after a sign-out the app
     * did not initiate, such as a revoked session.
     */
    suspend fun afterSignOut()
}

/**
 * Default [SignOutCleanup] implementation.
 *
 * Enrollments are stored on the device rather than on the account, which is why they are removed
 * here too. Clearing the device's study state never fails: each part is attempted even when another
 * one fails, and failures are logged.
 */
@Suppress("detekt:LongParameterList")
class SignOutCleanupImpl(
    private val healthData: MHCHealthDataHandler,
    private val studyManager: StudyManager,
    private val studyActivation: StudyActivation,
    private val studyVariants: StudyVariantProvider,
    private val onboardingCompletion: OnboardingCompletion,
    private val firebaseLoader: MHCFirebaseLoader,
) : SignOutCleanup {

    private val logger by mhcFirebaseLogger()

    override suspend fun beforeSignOut() {
        clearLocalStudyState()
        // Firestore refuses to clear its cache while its client is running, so the documents of the
        // participant signing out here can only go on the next process start.
        firebaseLoader.setShouldClearCacheOnNextLaunch(shouldClear = true)
    }

    override suspend fun afterSignOut() {
        val wasOnboarded = onboardingCompletion.isComplete
        clearLocalStudyState()
        // Whoever signs in next starts from the beginning of onboarding, not from the study.
        onboardingCompletion.reset()
        // Without an enrollment there is nothing tied to this region or variant, so the next
        // participant picks their own during onboarding rather than inheriting them.
        if (wasOnboarded && studyManager.studyEnrollments().isEmpty()) {
            firebaseLoader.clearRegion()
            studyVariants.clear()
        }
    }

    private suspend fun clearLocalStudyState() {
        runCatchingAsync { healthData.clearPendingUploads() }
            .onFailure { logger.e(it) { "Clearing the pending uploads failed." } }
        runCatchingAsync { studyManager.studyEnrollments().forEach { studyManager.unenroll(enrollment = it) } }
            .onFailure { logger.e(it) { "Removing the study enrollments failed." } }
        studyActivation.clear()
    }
}
