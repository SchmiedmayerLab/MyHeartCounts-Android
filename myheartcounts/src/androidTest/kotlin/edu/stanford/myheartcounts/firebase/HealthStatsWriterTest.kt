//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.Source
import edu.stanford.myheartcounts.healthstats.HealthStatsContribution
import edu.stanford.myheartcounts.healthstats.HealthStatsEntry
import edu.stanford.myheartcounts.healthstats.HealthStatsEntryCollection
import edu.stanford.myheartcounts.healthstats.HealthStatsWriter
import edu.stanford.myheartcounts.model.HealthUnit
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.grovealliance.core.requireDependency
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class HealthStatsWriterTest {
    private val writer: HealthStatsWriter get() = requireDependency()

    @Before
    fun signIn() = runTest {
        assumeTrue("Needs -Pmhc.firebaseEmulatorHost", FirebaseTestSession.isEmulatorConfigured)
        FirebaseTestSession.initializeFirebase()
        FirebaseTestSession.signUpFreshParticipant()
    }

    @After
    fun signOut() = runTest {
        if (FirebaseTestSession.isEmulatorConfigured) FirebaseTestSession.signOut()
    }

    @Test
    fun replacesOnlyItsOwnDottedSourceWithoutDuplicatingEntries() = runTest {
        // given
        val document = FirebaseTestSession.firestore.healthStatsMonths(metricId = "weight").document("2026-01")
        val otherEntries = listOf(mapOf("date" to "2026-01-01T12:00:00Z", "unit" to "kg", "value" to 72.0))
        document.set(mapOf("version" to 0, "metric" to "weight", "samples" to mapOf(OTHER_SOURCE to otherEntries))).await()

        // when
        writer.write(contribution = contribution(value = 80.0)).getOrThrow()
        writer.write(contribution = contribution(value = 81.0)).getOrThrow()
        writer.write(contribution = contribution(value = 81.0)).getOrThrow()

        // then
        val stored = document.get(Source.SERVER).await()
        val sources = stored.get("samples") as Map<*, *>
        assertThat(sources.keys).containsExactly(OTHER_SOURCE, ANDROID_SOURCE)
        assertThat(sources[OTHER_SOURCE]).isEqualTo(otherEntries)
        val ownEntries = sources[ANDROID_SOURCE] as List<*>
        assertThat(ownEntries).hasSize(1)
        assertThat((ownEntries.single() as Map<*, *>)["value"]).isEqualTo(81.0)
        assertThat(stored.getLong("version")).isEqualTo(0L)
        assertThat(stored.getString("metric")).isEqualTo("weight")
    }

    @Test
    fun clearsOnlyItsOwnContribution() = runTest {
        // given
        val document = FirebaseTestSession.firestore.healthStatsMonths(metricId = "weight").document("2026-01")
        val otherEntries = listOf(mapOf("date" to "2026-01-01T12:00:00Z", "unit" to "kg", "value" to 72.0))
        document.set(mapOf("version" to 0, "metric" to "weight", "samples" to mapOf(OTHER_SOURCE to otherEntries))).await()
        writer.write(contribution = contribution(value = 80.0)).getOrThrow()

        // when
        writer.write(contribution = contribution(value = null)).getOrThrow()

        // then
        val sources = document.get(Source.SERVER).await().get("samples") as Map<*, *>
        assertThat(sources[ANDROID_SOURCE] as List<*>).isEmpty()
        assertThat(sources[OTHER_SOURCE]).isEqualTo(otherEntries)
    }

    @Test
    fun clearingMissingMonthDoesNotCreateDocument() = runTest {
        // given
        val document = FirebaseTestSession.firestore.healthStatsMonths(metricId = "weight").document("2026-01")
        assertThat(document.get(Source.SERVER).await().exists()).isFalse()

        // when
        writer.write(contribution = contribution(value = null)).getOrThrow()

        // then
        assertThat(document.get(Source.SERVER).await().exists()).isFalse()
    }

    private fun contribution(value: Double?) = HealthStatsContribution(
        metricId = "weight",
        month = YearMonth.of(2026, 1),
        collection = HealthStatsEntryCollection.Samples,
        sourceId = ANDROID_SOURCE,
        entries = value?.let {
            listOf(HealthStatsEntry.Quantity(
                date = Instant.parse("2026-01-01T12:00:00Z"),
                unit = HealthUnit.Kilogram,
                value = it,
            ))
        }.orEmpty(),
    )

    private companion object {
        const val ANDROID_SOURCE = "androidx.health.connect"
        const val OTHER_SOURCE = "com.apple.HealthKit"
    }
}
