//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.study

import org.grovealliance.studydefinition.StudyBundle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.util.UUID

/**
 * An in-memory [StudyManager] for tests.
 *
 * Enrolling records a [StudyEnrollment] derived from the bundle; unenrolling removes it. No scheduler
 * tasks are created — tests that need the derived tasks should drive the production manager against a
 * [org.grovealliance.scheduler.FakeScheduler].
 */
class FakeStudyManager : StudyManager {

    private val state = MutableStateFlow<List<StudyEnrollment>>(emptyList())

    override val enrollments: Flow<List<StudyEnrollment>> = state.asStateFlow()

    /**
     * Sets the enrollments this manager reports.
     */
    fun setEnrollments(enrollments: List<StudyEnrollment>) {
        state.value = enrollments
    }

    override suspend fun studyEnrollments(): List<StudyEnrollment> = state.value

    override suspend fun enrollment(id: UUID): StudyEnrollment? = state.value.firstOrNull { it.id == id }

    override suspend fun enroll(studyBundle: StudyBundle, enrollmentDate: Instant?) {
        val definition = studyBundle.studyDefinition
        val existing = state.value.firstOrNull { it.studyId == definition.id }
        val enrollment = StudyEnrollment(
            id = existing?.id ?: UUID.randomUUID(),
            enrollmentDate = enrollmentDate ?: existing?.enrollmentDate ?: Instant.now(),
            studyId = definition.id,
            studyRevision = definition.studyRevision,
        )
        state.value = state.value.filterNot { it.studyId == definition.id } + enrollment
    }

    override suspend fun unenroll(enrollment: StudyEnrollment) {
        state.value = state.value.filterNot { it.id == enrollment.id }
    }

    override suspend fun informAboutStudies(studyBundles: Collection<StudyBundle>) {
        for (bundle in studyBundles) {
            state.value = state.value.map { enrollment ->
                if (enrollment.studyId == bundle.id) {
                    enrollment.copy(studyRevision = bundle.studyDefinition.studyRevision)
                } else {
                    enrollment
                }
            }
        }
    }

    override suspend fun removeOrphanedTasks() = Unit

    override suspend fun removeOrphanedStudyBundles() = Unit
}
