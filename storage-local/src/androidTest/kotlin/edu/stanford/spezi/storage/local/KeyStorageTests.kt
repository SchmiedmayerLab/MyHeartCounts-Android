//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.storage.local

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KeyStorageTests {

    private val keyStorage: KeyStorage = KeyStorageImpl()

    private val keyName = "TestKey"

    @Test
    fun `it should create keys correctly`() {
        // given
        val key = keyStorage.create(keyName).getOrThrow()

        // when
        val keyPair = keyStorage.retrieveKeyPair(keyName)
        val privateKey = keyStorage.retrievePrivateKey(keyName)
        val publicKey = keyStorage.retrievePublicKey(keyName)

        // then
        assertThat(privateKey).isEqualTo(key.private)
        assertThat(privateKey).isEqualTo(keyPair?.private)
        assertThat(publicKey).isEqualTo(key.public)
        assertThat(publicKey).isEqualTo(keyPair?.public)
    }

    @Test
    fun `it should handle key deletion correctly`() {
        // given
        keyStorage.create(keyName)

        // when
        keyStorage.delete(keyName)
        val privateKey = keyStorage.retrievePrivateKey(keyName)
        val publicKey = keyStorage.retrievePublicKey(keyName)

        // then
        assertThat(privateKey).isNull()
        assertThat(publicKey).isNull()
    }

    @Test
    fun `it should handle clear correctly`() {
        // given
        keyStorage.create(keyName)

        // when
        keyStorage.deleteAll()

        // then
        assertThat(keyStorage.retrieveKeyPair(keyName)).isNull()
    }
}
