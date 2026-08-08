//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import edu.stanford.myheartcounts.account.DateOfEnrollmentKey
import edu.stanford.myheartcounts.account.dateOfEnrollment
import edu.stanford.spezi.account.Account
import edu.stanford.spezi.account.AccountDetails
import edu.stanford.spezi.account.AccountModifications
import edu.stanford.spezi.core.logging.speziLogger
import edu.stanford.spezi.core.time.TimeProvider
import edu.stanford.spezi.study.StudyEnrollmentException
import edu.stanford.spezi.study.StudyManager
import java.time.Instant

/**
 * Enrolls the participant into the study the app ships.
 */
interface StudyEnroller {
    /**
     * Enrolls into the study, anchoring schedules to the participant's original enrollment date when
     * one is already known.
     *
     * Enrolling again into a revision that is already enrolled is not an error.
     */
    suspend fun enroll(): Result<Unit>
}

/**
 * Enrolls using the loaded study bundle, treating the account's enrollment date as authoritative so
 * that relative schedules survive a reinstall.
 */
class StudyEnrollerImpl(
    private val studyBundleProvider: MHCStudyBundleProvider,
    private val studyManager: StudyManager,
    private val account: Account,
    private val timeProvider: TimeProvider,
) : StudyEnroller {
    private val logger by speziLogger()

    override suspend fun enroll(): Result<Unit> = runCatching {
        val bundle = studyBundleProvider.get().getOrThrow()

        val knownEnrollmentDate = account.details.value?.dateOfEnrollment
        val enrollmentDate = knownEnrollmentDate ?: timeProvider.nowInstant()

        try {
            studyManager.enroll(
                studyBundle = bundle,
                enrollmentDate = enrollmentDate,
            )
        } catch (_: StudyEnrollmentException.AlreadyEnrolledInNewerRevision) {
            logger.i { "Already enrolled in a newer revision; keeping the existing enrollment." }
        }

        if (knownEnrollmentDate == null) {
            recordEnrollmentDate(enrollmentDate)
        }
    }.onFailure { logger.e(it) { "Failed to enroll into the study" } }

    /**
     * Stores [date] on the account so a later enrollment reuses it.
     */
    private suspend fun recordEnrollmentDate(date: Instant) {
        if (!account.isSignedIn) return
        val modifiedDetails = AccountDetails()
        modifiedDetails[DateOfEnrollmentKey::class] = date
        AccountModifications(modifiedDetails = modifiedDetails)
            .mapCatching { account.service.updateAccountDetails(it).getOrThrow() }
            .onFailure { logger.e(it) { "Failed to record the enrollment date on the account" } }
    }
}
