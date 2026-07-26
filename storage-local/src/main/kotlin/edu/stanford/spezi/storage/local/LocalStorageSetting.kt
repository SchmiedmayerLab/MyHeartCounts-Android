//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.storage.local

import java.security.KeyPair

sealed interface LocalStorageSetting {
    data object Unencrypted : LocalStorageSetting
    data class Encrypted(val keyPair: KeyPair) : LocalStorageSetting
    data object EncryptedUsingKeyStore : LocalStorageSetting
}
