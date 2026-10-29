//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import com.google.common.truth.Truth.assertThat
import org.grovealliance.account.FakeAccount
import org.grovealliance.storage.local.InMemoryKeyValueStorage
import org.junit.Test

class HealthStatsCheckpointStoreTest {
    private val account = FakeAccount()
    private val storage = InMemoryKeyValueStorage()
    private val checkpoints = HealthStatsCheckpointStoreImpl(
        storage = storage,
        account = account,
    )

    @Test
    fun `checkpoints are stored in the current participant namespace`() {
        // given
        account.supplyUserDetails(details = HealthStatsWritingFixtures.account())
        val checkpoint = HealthStatsWritingFixtures.checkpoint()
        checkpoints.put(metricId = "steps", month = HealthStatsWritingFixtures.MONTH, checkpoint = checkpoint)

        // when
        account.supplyUserDetails(details = HealthStatsWritingFixtures.account(id = "another"))

        // then
        assertThat(checkpoints.get(metricId = "steps", month = HealthStatsWritingFixtures.MONTH)).isNull()

        // when
        account.supplyUserDetails(details = HealthStatsWritingFixtures.account())

        // then
        assertThat(checkpoints.get(metricId = "steps", month = HealthStatsWritingFixtures.MONTH)).isEqualTo(checkpoint)
    }
}
