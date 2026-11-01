//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.account.EnableDebugModeKey
import edu.stanford.myheartcounts.customsamples.CustomQuantitySampleType
import edu.stanford.myheartcounts.dashboard.FakeHeartHealthCustomSampleSource
import edu.stanford.myheartcounts.dashboard.HeartHealthScoreFixtures
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.grovealliance.account.FakeAccount
import org.junit.Test

class HeartScoreDebugSourceTest {
    private val account = FakeAccount()
    private val custom = FakeHeartHealthCustomSampleSource()
    private val source = HeartScoreDebugSourceImpl(
        account = account,
        customSamples = custom,
    )

    @Test
    fun `debug observations are not queried for ordinary participants`() = runTest {
        // given
        account.supplyUserDetails(details = HeartHealthScoreFixtures.account())

        // when
        val samples = source.samples(component = HeartScoreComponent.Diet).first()

        // then
        assertThat(samples).isNull()
        assertThat(custom.ranges).isEmpty()
    }

    @Test
    fun `enabled debug mode reads all custom observations without a time filter`() = runTest {
        // given
        account.supplyUserDetails(details = HeartHealthScoreFixtures.account().apply { this[EnableDebugModeKey::class] = true })
        custom.setSamples(type = CustomQuantitySampleType.DietScore, samples = listOf(
            HeartHealthScoreFixtures.custom(type = CustomQuantitySampleType.DietScore, value = 18.0),
        ))

        // when
        val samples = source.samples(component = HeartScoreComponent.Diet).first()

        // then
        assertThat(samples).hasSize(1)
        assertThat(custom.ranges).containsExactly(null)
    }

    @Test
    fun `native metrics have no custom sample browser`() = runTest {
        // when
        val samples = source.samples(component = HeartScoreComponent.Sleep).first()

        // then
        assertThat(samples).isNull()
        assertThat(custom.ranges).isEmpty()
    }
}
