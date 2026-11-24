//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health.exchange

import java.util.UUID
import javax.crypto.spec.SecretKeySpec

/**
 * Exchange identity secrets for tests, with a fixed key and store id.
 */
object FhirExchangeIdentitySecretFixtures {

    /**
     * A [FhirExchangeIdentitySecret] whose key is [keyByte] repeated.
     */
    fun create(
        keyByte: Byte = 1,
        storeId: UUID = UUID(0L, 1L),
        epoch: Long = 1,
    ): FhirExchangeIdentitySecret = FhirExchangeIdentitySecret(
        key = SecretKeySpec(ByteArray(size = 32) { keyByte }, "HmacSHA256"),
        storeId = storeId,
        epoch = epoch,
    )
}
