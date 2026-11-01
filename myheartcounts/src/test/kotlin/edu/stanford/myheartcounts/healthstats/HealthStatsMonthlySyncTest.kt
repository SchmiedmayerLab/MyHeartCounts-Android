//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.grovealliance.account.FakeAccount
import org.grovealliance.health.FakeHealth
import org.grovealliance.health.HealthChanges
import org.grovealliance.health.RecordType
import org.junit.Before
import org.junit.Test

class HealthStatsMonthlySyncTest {
    private val fixtures = HealthStatsWritingFixtures
    private val account = FakeAccount()
    private val health = FakeHealth()
    private val collector = FakeHealthStatsMonthCollector()
    private val persistence = FakeHealthStatsPersistence()
    private val environment = FakeHealthStatsSyncEnvironment()
    private val sync = HealthStatsMonthlySyncImpl(
        health = health,
        collector = collector,
        persistence = persistence,
        environment = environment,
        configuration = fixtures.configuration(),
        account = account,
    )

    @Before
    fun setup() {
        account.supplyUserDetails(details = fixtures.account())
        health.setAuthorizedToRead(types = setOf(RecordType.steps))
        health.setHistoryReadable(value = true)
        health.setBackgroundReadable(value = true)
        collector.setSnapshot(value = fixtures.snapshot(
            entries = listOf(HealthStatsFixtures.sumBucket(hour = 1.0, amount = 100.0)),
            ids = setOf("tracked-record"),
        ))
    }

    @Test
    fun `initial refresh writes the monthly source and commits its token`() = runTest {
        // when
        val result = refresh()

        // then
        assertThat(result.isSuccess).isTrue()
        assertThat(persistence.writes).hasSize(1)
        assertThat(persistence.writes.single().sourceId).isEqualTo(fixtures.configuration().sourceId)
        assertThat(checkpoint()?.tokens?.get(RecordType.steps.identifier)).isEqualTo("initial-token")
        assertThat(checkpoint()?.recordIds).containsExactly("tracked-record")
    }

    @Test
    fun `unchanged tokens avoid monthly reads and uploads`() = runTest {
        // given
        refresh()

        // when
        refresh()

        // then
        assertThat(collector.collectionCount).isEqualTo(1)
        assertThat(persistence.writes).hasSize(1)
    }

    @Test
    fun `a tracked deletion can clear the month after a full reread`() = runTest {
        // given
        refresh()
        collector.setSnapshot(value = fixtures.snapshot(entries = emptyList(), ids = emptySet()))
        changes(deleted = setOf("tracked-record"), reset = false)

        // when
        refresh()

        // then
        assertThat(collector.collectionCount).isEqualTo(2)
        assertThat(persistence.writes.last().entries).isEmpty()
        assertThat(checkpoint()?.tokens?.get(RecordType.steps.identifier)).isEqualTo("updated-token")
    }

    @Test
    fun `unrelated deletions do not clear or reread another month`() = runTest {
        // given
        refresh()
        changes(deleted = setOf("another-month-record"), reset = false)

        // when
        refresh()

        // then
        assertThat(collector.collectionCount).isEqualTo(1)
        assertThat(persistence.writes).hasSize(1)
        assertThat(checkpoint()?.recordIds).containsExactly("tracked-record")
    }

    @Test
    fun `expired token rereads without treating empty results as deletions`() = runTest {
        // given
        refresh()
        collector.setSnapshot(value = fixtures.snapshot(entries = emptyList(), ids = emptySet()))
        changes(deleted = emptySet(), reset = true)

        // when
        refresh()

        // then
        assertThat(collector.collectionCount).isEqualTo(2)
        assertThat(persistence.writes).hasSize(1)
        assertThat(checkpoint()?.tokens?.get(RecordType.steps.identifier)).isEqualTo("updated-token")
    }

    @Test
    fun `explicit refresh rereads even without device changes`() = runTest {
        // given
        refresh()

        // when
        sync.refresh(metric = HealthStatsSyncMetric.Steps, month = fixtures.month(), force = true)

        // then
        assertThat(collector.collectionCount).isEqualTo(2)
    }

    @Test
    fun `permission loss during a read preserves data and checkpoint`() = runTest {
        // given
        collector.setAfterCollect { environment.setReadable(value = false) }

        // when
        refresh()

        // then
        assertThat(persistence.writes).isEmpty()
        assertThat(checkpoint()).isNull()
    }

    @Test
    fun `unreadable data cannot be collected or uploaded`() = runTest {
        // given
        environment.setReadable(value = false)

        // when
        refresh()

        // then
        assertThat(collector.collectionCount).isEqualTo(0)
        assertThat(persistence.writes).isEmpty()
    }

    @Test
    fun `unreadable months never advance their checkpoints`() = runTest {
        // given
        environment.setReadable(value = false)

        // when
        refresh()

        // then
        assertThat(collector.collectionCount).isEqualTo(0)
        assertThat(checkpoint()).isNull()
    }

    @Test
    fun `account switch during a read cannot publish its snapshot`() = runTest {
        // given
        collector.setAfterCollect { account.supplyUserDetails(details = fixtures.account(id = "another")) }

        // when
        val result = refresh()

        // then
        assertThat(result.exceptionOrNull()).isInstanceOf(HealthStatsStoreException.SessionChanged::class.java)
        assertThat(persistence.writes).isEmpty()
        assertThat(checkpoint()).isNull()
    }

    private suspend fun refresh() = sync.refresh(metric = HealthStatsSyncMetric.Steps, month = fixtures.month(), force = false)

    private fun checkpoint() = persistence.checkpoint(metric = HealthStatsSyncMetric.Steps, month = fixtures.MONTH)

    private fun changes(deleted: Set<String>, reset: Boolean) {
        health.setChanges(type = RecordType.steps, result = Result.success(HealthChanges(
            added = emptyList(), deletedIds = deleted, nextToken = "updated-token", isReset = reset,
        )))
    }
}
