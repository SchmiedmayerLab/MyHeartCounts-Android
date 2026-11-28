//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health.exchange

import com.google.common.truth.Truth.assertThat
import org.grovealliance.storage.local.InMemoryKeyValueStorage
import org.junit.Test

class FhirExchangeIdentityStoreTest {

    private val storage = InMemoryKeyValueStorage()

    @Test
    fun `creates a 32-byte key at the first epoch`() {
        // when
        val secret = FhirExchangeIdentityStoreImpl(storage = storage).secret().getOrThrow()

        // then
        assertThat(secret.key.encoded).hasLength(32)
        assertThat(secret.epoch).isEqualTo(1L)
    }

    @Test
    fun `returns the same secret across instances`() {
        // given
        val created = FhirExchangeIdentityStoreImpl(storage = storage).secret().getOrThrow()

        // when
        val read = FhirExchangeIdentityStoreImpl(storage = storage).secret().getOrThrow()

        // then
        assertThat(read.key.encoded).isEqualTo(created.key.encoded)
        assertThat(read.storeId).isEqualTo(created.storeId)
        assertThat(read.epoch).isEqualTo(created.epoch)
    }

    @Test
    fun `creates a different secret for another install`() {
        // given
        val first = FhirExchangeIdentityStoreImpl(storage = storage).secret().getOrThrow()

        // when
        val other = FhirExchangeIdentityStoreImpl(storage = InMemoryKeyValueStorage()).secret().getOrThrow()

        // then
        assertThat(other.key.encoded).isNotEqualTo(first.key.encoded)
        assertThat(other.storeId).isNotEqualTo(first.storeId)
    }
}
