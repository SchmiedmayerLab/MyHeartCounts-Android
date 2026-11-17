//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.grovealliance.account.FakeAccount
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.storage.local.InMemoryKeyValueStorage
import org.grovealliance.study.FakeStudyManager
import org.grovealliance.studydefinition.StudyBundle
import org.grovealliance.studydefinition.fixtures.StudyBundleFixtures
import org.junit.Test
import java.time.Instant

class StudyEnrollerTest {

    private val studyManager = FakeStudyManager()
    private val timeProvider = FakeTimeProvider().apply {
        setNow(instant = Instant.parse("2026-01-01T09:30:00Z"))
    }
    private val studyActivation = StudyActivationImpl(
        storage = InMemoryKeyValueStorage(),
        timeProvider = timeProvider,
    )

    @Test
    fun `records the activation date once enrolled`() = runTest {
        // given
        val enroller = enroller(bundle = Result.success(StudyBundleFixtures.exampleBundle()))

        // when
        val result = enroller.enroll()

        // then
        assertThat(result.isSuccess).isTrue()
        assertThat(studyManager.studyEnrollments()).hasSize(1)
        assertThat(studyActivation.currentActivationDate).isEqualTo(Instant.parse("2026-01-01T09:30:00Z"))
    }

    @Test
    fun `records no activation date when enrolling fails`() = runTest {
        // given
        val enroller = enroller(bundle = Result.failure(IllegalStateException("not bundled")))

        // when
        val result = enroller.enroll()

        // then
        assertThat(result.isFailure).isTrue()
        assertThat(studyActivation.currentActivationDate).isNull()
    }

    private fun enroller(bundle: Result<StudyBundle>) = StudyEnrollerImpl(
        studyBundleProvider = object : MHCStudyBundleProvider {
            override suspend fun get(): Result<StudyBundle> = bundle
        },
        studyManager = studyManager,
        account = FakeAccount(),
        timeProvider = timeProvider,
        studyActivation = studyActivation,
    )
}
