//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.grovealliance.core.lifecycle.AppLifecycle
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.health.FakeHealth
import org.grovealliance.health.RecordType
import org.grovealliance.storage.local.InMemoryKeyValueStorage
import org.junit.Before
import org.junit.Test

class HealthStatsSyncEnvironmentTest {
    private val health = FakeHealth()
    private val timeProvider = FakeTimeProvider().apply { setNow(HealthStatsWritingFixtures.NOW) }
    private val lifecycle = AppLifecycle()
    private val environment = HealthStatsSyncEnvironmentImpl(
        health = health,
        timeProvider = timeProvider,
        appLifecycle = lifecycle,
        storage = InMemoryKeyValueStorage(),
    )

    @Before
    fun setup() {
        health.setAuthorizedToRead(types = setOf(RecordType.steps))
        health.setBackgroundReadable(value = true)
        health.setHistoryReadable(value = true)
    }

    @Test
    fun `history and background access permit a complete monthly read`() = runTest {
        // when / then
        assertThat(environment.canRead(metric = HealthStatsSyncMetric.Steps, month = HealthStatsWritingFixtures.month())).isTrue()
    }

    @Test
    fun `background reads require background permission`() = runTest {
        // given
        health.setBackgroundReadable(value = false)

        // when / then
        assertThat(environment.canRead(metric = HealthStatsSyncMetric.Steps, month = HealthStatsWritingFixtures.month())).isFalse()
    }

    @Test
    fun `a month outside the recent window requires history permission`() = runTest {
        // given
        health.setHistoryReadable(value = false)
        timeProvider.setNow(HealthStatsWritingFixtures.NOW.plusSeconds(40 * 86400))

        // when / then
        assertThat(environment.canRead(metric = HealthStatsSyncMetric.Steps, month = HealthStatsWritingFixtures.month())).isFalse()
    }

    @Test
    fun `a month starting within thirty days before the first read access stays readable without history permission`() = runTest {
        // given
        health.setHistoryReadable(value = false)
        environment.canRead(metric = HealthStatsSyncMetric.Steps, month = HealthStatsWritingFixtures.month())

        // when
        timeProvider.setNow(HealthStatsWritingFixtures.NOW.plusSeconds(40 * 86400))

        // then
        assertThat(environment.canRead(metric = HealthStatsSyncMetric.Steps, month = HealthStatsWritingFixtures.month())).isTrue()
    }
}
