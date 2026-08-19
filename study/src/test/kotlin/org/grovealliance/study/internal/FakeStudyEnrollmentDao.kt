//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.study.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An in-memory [StudyEnrollmentDao] for JVM tests.
 *
 * The DAO is an internal type, so its double stays local to this module's tests; only public-API
 * doubles are published via `testFixtures`.
 */
internal class FakeStudyEnrollmentDao : StudyEnrollmentDao {
    private val state = MutableStateFlow<List<StudyEnrollmentEntity>>(emptyList())

    override suspend fun all(): List<StudyEnrollmentEntity> = state.value

    override fun observeAll() = state.asStateFlow()

    override suspend fun byId(id: String): StudyEnrollmentEntity? = state.value.firstOrNull { it.id == id }

    override suspend fun byStudyId(studyId: String): List<StudyEnrollmentEntity> =
        state.value.filter { it.studyId == studyId }

    override suspend fun upsert(enrollment: StudyEnrollmentEntity) {
        state.value = state.value.filterNot { it.id == enrollment.id } + enrollment
    }

    override suspend fun updateRevision(id: String, revision: Long) {
        state.value = state.value.map { if (it.id == id) it.copy(studyRevision = revision) else it }
    }

    override suspend fun delete(id: String) {
        state.value = state.value.filterNot { it.id == id }
    }

    override suspend fun deleteAll() {
        state.value = emptyList()
    }
}
