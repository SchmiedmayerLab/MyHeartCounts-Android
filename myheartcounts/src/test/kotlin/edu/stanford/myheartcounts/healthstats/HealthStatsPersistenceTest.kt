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
import org.junit.Before
import org.junit.Test
import java.io.IOException

class HealthStatsPersistenceTest {
    private val fixtures = HealthStatsWritingFixtures
    private val account = FakeAccount()
    private val writer = FakeHealthStatsWriter()
    private val checkpoints = FakeHealthStatsCheckpointStore()
    private val persistence = HealthStatsPersistenceImpl(
        writer = writer,
        checkpoints = checkpoints,
        account = account,
    )

    @Before
    fun setup() {
        account.supplyUserDetails(details = fixtures.account())
    }

    @Test
    fun `empty reads preserve existing source data while acknowledging the read`() = runTest {
        // when
        persistence.persist(contribution = fixtures.contribution(
            entries = emptyList(),
        ), checkpoint = fixtures.checkpoint(), hasDeletions = false)

        // then
        assertThat(writer.writes).isEmpty()
        assertThat(checkpoints.get(metricId = "steps", month = fixtures.MONTH)).isEqualTo(fixtures.checkpoint())
    }

    @Test
    fun `confirmed deletions can clear an existing source contribution`() = runTest {
        // when
        persistence.persist(contribution = fixtures.contribution(
            entries = emptyList(),
        ), checkpoint = fixtures.checkpoint(), hasDeletions = true)

        // then
        assertThat(writer.writes).containsExactly(fixtures.contribution(entries = emptyList()))
        assertThat(checkpoints.get(metricId = "steps", month = fixtures.MONTH)).isNotNull()
    }

    @Test
    fun `failed uploads never advance the checkpoint`() = runTest {
        // given
        writer.setFailure(error = IOException("offline"))

        // when
        val result = persistence.persist(
            contribution = fixtures.contribution(entries = listOf(HealthStatsFixtures.sumBucket(hour = 1.0, amount = 100.0))),
            checkpoint = fixtures.checkpoint(),
            hasDeletions = false,
        )

        // then
        assertThat(result.isFailure).isTrue()
        assertThat(checkpoints.get(metricId = "steps", month = fixtures.MONTH)).isNull()
    }

    @Test
    fun `account switches during acknowledgement cannot save the previous checkpoint`() = runTest {
        // given
        writer.setAfterWrite { account.supplyUserDetails(details = fixtures.account(id = "another")) }

        // when
        val result = persistence.persist(
            contribution = fixtures.contribution(entries = listOf(HealthStatsFixtures.sumBucket(hour = 1.0, amount = 100.0))),
            checkpoint = fixtures.checkpoint(),
            hasDeletions = false,
        )

        // then
        assertThat(result.exceptionOrNull()).isInstanceOf(HealthStatsStoreException.SessionChanged::class.java)
        assertThat(checkpoints.get(metricId = "steps", month = fixtures.MONTH)).isNull()
    }
}
