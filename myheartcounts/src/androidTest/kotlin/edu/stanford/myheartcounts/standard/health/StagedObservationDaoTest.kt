//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the staged-observation queries against a real SQLite database.
 */
@RunWith(AndroidJUnit4::class)
class StagedObservationDaoTest {

    private lateinit var database: HealthStagingDatabase
    private val dao: StagedObservationDao get() = database.observations()

    @Before
    fun openDatabase() {
        database = Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), HealthStagingDatabase::class.java)
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun removesADeletedRecordTogetherWithItsReadingsOnly() = runTest {
        dao.stage(observations = listOf("rec1", "rec1-100", "rec1-200", "rec10", "rec1x-100", "rec2").map(::staged))

        val removed = dao.removeObservationsOfRecord(recordId = "rec1")

        assertThat(removed).isEqualTo(3)
        assertThat(remainingIds()).containsExactly("rec10", "rec1x-100", "rec2")
    }

    @Test
    fun matchesLikeWildcardsInARecordIdLiterally() = runTest {
        dao.stage(observations = listOf("a%", "a%-1", "ab-1", "a_b-1", "axb-1").map(::staged))

        dao.removeObservationsOfRecord(recordId = "a%")
        dao.removeObservationsOfRecord(recordId = "a_b")

        assertThat(remainingIds()).containsExactly("ab-1", "axb-1")
    }

    @Test
    fun findsWhichTrackedReadingsAreStillStaged() = runTest {
        dao.stage(observations = listOf("rec-1", "rec-2").map(::staged))

        val stillStaged = database.seriesReadings().stagedObservationIdsAmong(ids = listOf("rec-1", "rec-3"))

        assertThat(stillStaged).containsExactly("rec-1")
    }

    @Test
    fun forgetsSeriesReadingsDeliveredBeforeTheCutoff() = runTest {
        val series = database.seriesReadings()
        series.save(
            readings = listOf(
                SeriesRecordReadings("old", SAMPLE_TYPE, observationIds = "old-1", updatedAtMillis = 10),
                SeriesRecordReadings("new", SAMPLE_TYPE, observationIds = "new-1", updatedAtMillis = 30),
            ),
        )

        series.removeOlderThan(cutoffMillis = 20)

        assertThat(series.readings(recordId = "old", sampleTypeIdentifier = SAMPLE_TYPE)).isNull()
        assertThat(series.readings(recordId = "new", sampleTypeIdentifier = SAMPLE_TYPE)).isNotNull()
    }

    @Test
    fun removesMoreIdsThanOneStatementBinds() = runTest {
        val ids = (0 until ID_COUNT).map { "id$it" }
        dao.stage(observations = ids.map(::staged))

        dao.removeObservations(ids = ids)

        assertThat(remainingIds()).isEmpty()
    }

    @Test
    fun dropsOnlyWhatAnotherAccountLeftStaged() = runTest {
        dao.stage(observations = listOf(staged(id = "mine"), staged(id = "theirs", accountId = OTHER_ACCOUNT)))

        dao.removeObservationsNotOwnedBy(accountId = ACCOUNT)

        assertThat(remainingIds()).containsExactly("mine")
        assertThat(remainingIds(accountId = OTHER_ACCOUNT)).isEmpty()
    }

    private suspend fun remainingIds(accountId: String = ACCOUNT): List<String> = dao.observationsReadyForUpload(
        accountId = accountId,
        sampleTypeIdentifier = SAMPLE_TYPE,
        cutoffMillis = Long.MAX_VALUE,
        limit = Int.MAX_VALUE,
    ).map { it.id }

    private fun staged(id: String, accountId: String = ACCOUNT) = StagedObservation(
        id = id,
        sampleTypeIdentifier = SAMPLE_TYPE,
        accountId = accountId,
        json = "{}",
        effectiveAtMillis = 0,
    )

    private companion object {
        const val SAMPLE_TYPE = "HKQuantityTypeIdentifierHeartRate"
        const val ACCOUNT = "account"
        const val OTHER_ACCOUNT = "other-account"
        const val ID_COUNT = 1_200
    }
}
