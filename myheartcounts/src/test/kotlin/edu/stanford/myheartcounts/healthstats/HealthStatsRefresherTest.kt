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
import org.junit.Before
import org.junit.Test
import java.io.IOException

class HealthStatsRefresherTest {
    private val account = FakeAccount()
    private val history = FakeHealthStatsHistory()
    private val health = FakeHealth()
    private val monthlySync = FakeHealthStatsMonthlySync()
    private val writer = FakeHealthStatsWriter()
    private val configuration = HealthStatsWritingFixtures.configuration()
    private val refresher = HealthStatsRefresherImpl(
        account = account,
        history = history,
        health = health,
        monthlySync = monthlySync,
        configuration = configuration,
        writer = writer,
    )

    @Before
    fun setup() {
        account.supplyUserDetails(details = HealthStatsWritingFixtures.account())
    }

    @Test
    fun `maintains all supported metrics for every history month`() = runTest {
        // when
        val result = refresher.refresh(force = false)

        // then
        assertThat(result.isSuccess).isTrue()
        assertThat(monthlySync.metrics).containsExactlyElementsIn(HealthStatsSyncMetric.entries).inOrder()
    }

    @Test
    fun `one metric failing does not prevent the others from being attempted`() = runTest {
        // given
        val failure = IOException("unavailable")
        monthlySync.setFailure(error = failure)

        // when
        val result = refresher.refresh(force = true)

        // then
        assertThat(result.exceptionOrNull()).isSameInstanceAs(failure)
        assertThat(monthlySync.metrics).containsExactlyElementsIn(HealthStatsSyncMetric.entries)
    }

    @Test
    fun `sign out stops further monthly refreshes`() = runTest {
        // given
        monthlySync.setAfterRefresh { account.removeUserDetails() }

        // when
        val result = refresher.refresh(force = false)

        // then
        assertThat(result.exceptionOrNull()).isInstanceOf(HealthStatsStoreException.SessionChanged::class.java)
        assertThat(monthlySync.metrics).hasSize(1)
    }

    @Test
    fun `signed out participants do not refresh`() = runTest {
        // given
        account.removeUserDetails()

        // when
        val result = refresher.refresh(force = false)

        // then
        assertThat(result.isSuccess).isTrue()
        assertThat(monthlySync.metrics).isEmpty()
    }
}
