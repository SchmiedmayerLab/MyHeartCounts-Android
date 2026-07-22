//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.study

import edu.stanford.spezi.core.Module
import edu.stanford.spezi.studydefinition.StudyBundle
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID

/**
 * Manages enrollment and participation in studies.
 *
 * Enrolling into a study compiles its component schedules into scheduler tasks; unenrolling removes
 * them. The manager keeps its enrollments across launches and reconciles the derived tasks whenever
 * a study's definition changes.
 */
interface StudyManager : Module {

    /**
     * A live stream of the current enrollments, re-emitting whenever they change.
     */
    val enrollments: Flow<List<StudyEnrollment>>

    /**
     * A snapshot of the current enrollments.
     */
    suspend fun studyEnrollments(): List<StudyEnrollment>

    /**
     * Enrolls into the study defined by [studyBundle], anchoring relative schedules to
     * [enrollmentDate] (the current time when `null`).
     *
     * Enrolling again into the same study is a no-op at the same revision, forwards to
     * [informAboutStudies] for a newer revision, and throws
     * [StudyEnrollmentException.AlreadyEnrolledInNewerRevision] for an older one. Throws
     * [StudyEnrollmentException.MissingStudyDependency] when the study depends on another the
     * participant is not enrolled in.
     */
    suspend fun enroll(studyBundle: StudyBundle, enrollmentDate: Instant? = null)

    /**
     * Unenrolls from [enrollment], deleting its tasks, stored bundle, and enrollment record.
     */
    suspend fun unenroll(enrollment: StudyEnrollment)

    /**
     * Updates any existing enrollment whose study matches one of [studyBundles] and whose revision
     * is older, re-compiling its tasks against the newer definition.
     */
    suspend fun informAboutStudies(studyBundles: Collection<StudyBundle>)

    /**
     * The enrollment with [id], or `null` if none exists.
     */
    suspend fun enrollment(id: UUID): StudyEnrollment?

    /**
     * Deletes study-domain tasks that no longer correspond to any current enrollment.
     */
    suspend fun removeOrphanedTasks()

    /**
     * Deletes stored study bundles that no longer correspond to any current enrollment.
     */
    suspend fun removeOrphanedStudyBundles()
}

/**
 * An error preventing enrollment into a study.
 */
sealed class StudyEnrollmentException(message: String) : Exception(message) {
    /**
     * Enrollment was attempted into an older revision than the one already enrolled in.
     */
    class AlreadyEnrolledInNewerRevision :
        StudyEnrollmentException("Already enrolled in a newer revision of this study.")

    /**
     * The study depends on another study the participant is not enrolled in.
     */
    class MissingStudyDependency :
        StudyEnrollmentException("Cannot enroll: a required study dependency is missing.")
}
