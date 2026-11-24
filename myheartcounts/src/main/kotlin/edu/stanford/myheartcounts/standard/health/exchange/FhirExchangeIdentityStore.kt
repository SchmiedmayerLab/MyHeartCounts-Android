//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health.exchange

import org.grovealliance.storage.local.KeyValueStorage
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * The install's FHIR exchange identity secret: created once, then the same for every upload, so a
 * record converted twice gets the same identities.
 */
interface FhirExchangeIdentityStore {

    /**
     * The install's secret, created on first use.
     */
    fun secret(): Result<FhirExchangeIdentitySecret>
}

/**
 * The HMAC [key] every exchange identity is minted with, the [storeId] naming this install's Health
 * Connect store, and the key [epoch].
 */
data class FhirExchangeIdentitySecret(
    val key: SecretKey,
    val storeId: UUID,
    val epoch: Long,
)

/**
 * [FhirExchangeIdentityStore] keeping the secret in encrypted [storage].
 */
class FhirExchangeIdentityStoreImpl(
    private val storage: KeyValueStorage,
) : FhirExchangeIdentityStore {
    private val lock = Any()

    override fun secret(): Result<FhirExchangeIdentitySecret> = synchronized(lock) {
        runCatching { storage.getString(key = STORAGE_KEY)?.let { decoded(stored = it) } ?: created() }
    }

    private fun created(): FhirExchangeIdentitySecret {
        val bytes = ByteArray(KEY_BYTES).also { SecureRandom().nextBytes(it) }
        val secret = FhirExchangeIdentitySecret(
            key = SecretKeySpec(bytes, ALGORITHM),
            storeId = UUID.randomUUID(),
            epoch = FIRST_EPOCH,
        )
        storage.putString(
            key = STORAGE_KEY,
            value = listOf(
                Base64.getEncoder().encodeToString(bytes),
                secret.storeId.toString(),
                secret.epoch.toString(),
            ).joinToString(separator = SEPARATOR),
        )
        return secret
    }

    private fun decoded(stored: String): FhirExchangeIdentitySecret {
        val (key, storeId, epoch) = stored.split(SEPARATOR)
        return FhirExchangeIdentitySecret(
            key = SecretKeySpec(Base64.getDecoder().decode(key), ALGORITHM),
            storeId = UUID.fromString(storeId),
            epoch = epoch.toLong(),
        )
    }

    private companion object {
        const val STORAGE_KEY = "fhir_exchange_identity"
        const val ALGORITHM = "HmacSHA256"
        const val KEY_BYTES = 32
        const val FIRST_EPOCH = 1L
        const val SEPARATOR = ":"
    }
}
