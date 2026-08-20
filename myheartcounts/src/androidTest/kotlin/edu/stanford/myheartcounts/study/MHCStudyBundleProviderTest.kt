//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.grovealliance.study.FakeStudyManager
import org.grovealliance.study.fixtures.StudyEnrollmentFixtures
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Exercises the study bundle as the app loads it at runtime: read from the packaged assets through a
 * real [Context], unpacked to disk, and decoded.
 */
@RunWith(AndroidJUnit4::class)
class MHCStudyBundleProviderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val studyManager = FakeStudyManager()
    private val scope = TestScope(StandardTestDispatcher())
    private val provider = MHCStudyBundleProviderImpl(
        context = context,
        studyManager = studyManager,
        scope = scope,
    )

    @Test
    fun `loads the packaged study bundle`() = scope.runTest {
        // when
        val bundle = provider.get().getOrThrow()

        // then
        assertThat(bundle.studyDefinition.studyRevision.toLong()).isAtLeast(MINIMUM_REVISION)
        assertThat(bundle.studyDefinition.components).isNotEmpty()
        assertThat(bundle.studyDefinition.componentSchedules).isNotEmpty()
    }

    @Test
    fun `resolves content the definition references`() = scope.runTest {
        // when
        val bundle = provider.get().getOrThrow()

        // then
        assertThat(bundle.consentText(Locale.US)).isNotEmpty()
        assertThat(bundle.studyDefinition.metadata.title.resolve(Locale.US)).isNotEmpty()
    }

    @Test
    fun `serves the same bundle to every caller`() = scope.runTest {
        // when
        val first = provider.get().getOrThrow()
        val second = provider.get().getOrThrow()

        // then
        assertThat(first).isSameInstanceAs(second)
    }

    @Test
    fun `carries an existing enrollment forward on configure`() = scope.runTest {
        // given
        val bundle = provider.get().getOrThrow()
        studyManager.setEnrollments(
            listOf(
                StudyEnrollmentFixtures.create(
                    studyId = bundle.id,
                    studyRevision = OUTDATED_REVISION,
                )
            )
        )

        // when
        provider.configure()
        advanceUntilIdle()

        // then
        assertThat(studyManager.studyEnrollments().single().studyRevision)
            .isEqualTo(bundle.studyDefinition.studyRevision)
    }

    private companion object {
        /**
         * The oldest revision known to decode. Raise this only alongside a bundle update.
         */
        const val MINIMUM_REVISION = 42L

        /**
         * A revision older than any the bundle carries, so an update is observable.
         */
        const val OUTDATED_REVISION = 1u
    }
}
