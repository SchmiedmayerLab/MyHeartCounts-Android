//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.grovealliance.foundation.fixtures.UUIDFixtures
import org.grovealliance.study.FakeStudyManager
import org.grovealliance.study.fixtures.StudyEnrollmentFixtures
import org.grovealliance.studydefinition.StudyBundle
import org.grovealliance.studydefinition.fixtures.StudyBundleFixtures
import org.junit.Test
import java.time.Instant

class EnrolledStudySourceTest {

    private val bundle = StudyBundleFixtures.exampleBundle()
    private val studyManager = FakeStudyManager()
    private val variantProvider = FakeStudyVariantProvider()
    private val source = EnrolledStudySourceImpl(
        studyManager = studyManager,
        studyBundleProvider = object : MHCStudyBundleProvider {
            override suspend fun get(): Result<StudyBundle> = Result.success(bundle)
        },
        variantProvider = variantProvider,
    )

    @Test
    fun `describes no study while not enrolled`() = runTest {
        // when
        val enrolledStudy = source.enrolledStudy().first()

        // then
        assertThat(enrolledStudy).isNull()
    }

    @Test
    fun `describes the enrolled study from the study bundle`() = runTest {
        // given
        studyManager.setEnrollments(
            enrollments = listOf(
                StudyEnrollmentFixtures.create(
                    enrollmentDate = Instant.parse("2026-01-01T09:30:00Z"),
                    studyId = bundle.studyDefinition.id,
                ),
            ),
        )

        // when
        val enrolledStudy = source.enrolledStudy().first()

        // then
        assertThat(enrolledStudy).isEqualTo(
            EnrolledStudyFixtures.create(
                title = "Example Study",
                shortExplanation = "An example study.",
                enrollmentDate = Instant.parse("2026-01-01T09:30:00Z"),
            ),
        )
    }

    @Test
    fun `describes an enrollment in a study the bundle does not describe without its description`() = runTest {
        // given
        studyManager.setEnrollments(
            enrollments = listOf(
                StudyEnrollmentFixtures.create(
                    enrollmentDate = Instant.parse("2026-01-01T09:30:00Z"),
                    studyId = UUIDFixtures.zero,
                ),
            ),
        )

        // when
        val enrolledStudy = source.enrolledStudy().first()

        // then
        assertThat(enrolledStudy).isEqualTo(
            EnrolledStudyFixtures.create(enrollmentDate = Instant.parse("2026-01-01T09:30:00Z")),
        )
    }
}
