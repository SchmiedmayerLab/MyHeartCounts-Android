//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.featureflag

import com.google.common.truth.Truth.assertThat
import org.grovealliance.storage.local.InMemoryKeyValueStorage
import org.junit.Test

class FeatureFlagsTest {

    private val storage = InMemoryKeyValueStorage()

    @Test
    fun `answers each flag's default value without stored flags`() {
        // when
        val values = FeatureFlag.entries.associateWith { DefaultFeatureFlags().isEnabled(flag = it) }

        // then
        assertThat(values).containsExactlyEntriesIn(FeatureFlag.entries.associateWith { it.defaultValue })
    }

    @Test
    fun `ignores stored values without stored flags`() {
        // given
        storage.putBoolean(key = FeatureFlag.SimplifyOnboarding.id, value = true)

        // when
        val isEnabled = DefaultFeatureFlags().isEnabled(flag = FeatureFlag.SimplifyOnboarding)

        // then
        assertThat(isEnabled).isFalse()
    }

    @Test
    fun `answers the value a flag was set to`() {
        // given
        val flags = StoredFeatureFlags(storage = storage)
        flags.setEnabled(
            flag = FeatureFlag.SimplifyOnboarding,
            enabled = true,
        )

        // when
        val isEnabled = flags.isEnabled(flag = FeatureFlag.SimplifyOnboarding)

        // then
        assertThat(isEnabled).isTrue()
    }

    @Test
    fun `keeps a set flag across instances sharing the storage`() {
        // given
        StoredFeatureFlags(storage = storage).setEnabled(
            flag = FeatureFlag.SimplifyOnboarding,
            enabled = true,
        )

        // when
        val isEnabled = StoredFeatureFlags(storage = storage).isEnabled(flag = FeatureFlag.SimplifyOnboarding)

        // then
        assertThat(isEnabled).isTrue()
    }

    @Test
    fun `answers a flag's default value when nothing is stored`() {
        // when
        val isEnabled = StoredFeatureFlags(storage = storage).isEnabled(flag = FeatureFlag.SimplifyOnboarding)

        // then
        assertThat(isEnabled).isEqualTo(FeatureFlag.SimplifyOnboarding.defaultValue)
    }
}
