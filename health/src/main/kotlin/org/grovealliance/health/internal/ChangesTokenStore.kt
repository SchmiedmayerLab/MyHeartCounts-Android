//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.internal

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.grovealliance.health.AnyRecordType
import org.grovealliance.health.HealthConstraint
import org.grovealliance.storage.local.LocalStorage
import org.grovealliance.storage.local.LocalStorageSetting
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Internal class for storing and retrieving health changes tokens.
 *
 * @property storage The [LocalStorage] instance used for storing tokens.
 */
@Suppress("TooManyFunctions")
internal class ChangesTokenStore(
    private val storage: LocalStorage,
) {
    private val ownerMutex = Mutex()

    /**
     * Stores a changes token for the specified [recordType].
     *
     * @param recordType The [AnyRecordType] for which the token is stored.
     * @param token The changes token to store.
     */
    suspend fun storeToken(recordType: AnyRecordType, token: String, collectionScopeId: String = "default") {
        val lease = claimProjection(recordType, HealthConstraint.DEFAULT_REPOSITORY_SCOPE_ID, collectionScopeId)
        storePendingBoundary(recordType, lease, token)
        commitToken(recordType, lease, token)
    }

    /**
     * Retrieves the changes token for the specified [recordType].
     *
     * @param recordType The [AnyRecordType] for which the token is retrieved.
     * @return The changes token, or null if not found.
     */
    suspend fun getToken(
        recordType: AnyRecordType,
        collectionScopeId: String = "default",
        repositoryScopeId: String = HealthConstraint.DEFAULT_REPOSITORY_SCOPE_ID,
    ): String? = getState(recordType, collectionScopeId, repositoryScopeId)?.token

    suspend fun getState(
        recordType: AnyRecordType,
        collectionScopeId: String,
        repositoryScopeId: String = HealthConstraint.DEFAULT_REPOSITORY_SCOPE_ID,
    ): ChangesTokenState? {
        return storage.read(
            key = keyFor(recordType, repositoryScopeId, collectionScopeId),
            settings = LocalStorageSetting.Unencrypted,
            serializer = serializer(),
        )
    }

    suspend fun getState(recordType: AnyRecordType, lease: CollectionProjectionLease): ChangesTokenState? =
        ownerMutex.withLock {
            requireOwner(recordType, lease)
            getState(recordType, lease.projectionScopeId, lease.repositoryScopeId)
        }

    /**
     * Deletes the changes token for the specified [recordType].
     *
     * @param recordType The [AnyRecordType] for which the token is deleted.
     */
    suspend fun deleteToken(
        recordType: AnyRecordType,
        collectionScopeId: String = "default",
        repositoryScopeId: String = HealthConstraint.DEFAULT_REPOSITORY_SCOPE_ID,
    ) {
        storage.delete(keyFor(recordType, repositoryScopeId, collectionScopeId))
    }

    /** Claims exclusive ownership of one repository/type for a projection generation. */
    suspend fun claimProjection(
        recordType: AnyRecordType,
        repositoryScopeId: String,
        projectionScopeId: String,
    ): CollectionProjectionLease = ownerMutex.withLock {
        require(repositoryScopeId.isNotBlank() && projectionScopeId.isNotBlank()) {
            "Repository and projection scope ids are required."
        }
        val current = readOwner(recordType, repositoryScopeId)
        val owner = if (current?.projectionScopeId == projectionScopeId) {
            current
        } else {
            CollectionProjectionOwner(
                projectionScopeId = projectionScopeId,
                generation = (current?.generation ?: 0L) + 1L,
                baselineRequired = true,
            ).also { storeOwner(recordType, repositoryScopeId, it) }
        }
        owner.lease(repositoryScopeId)
    }

    suspend fun baselineRequired(recordType: AnyRecordType, lease: CollectionProjectionLease): Boolean =
        ownerMutex.withLock { requireOwner(recordType, lease).baselineRequired }

    /** Stores the boundary before clearing the durable baseline-required marker. */
    suspend fun storePendingBoundary(
        recordType: AnyRecordType,
        lease: CollectionProjectionLease,
        token: String,
    ) = ownerMutex.withLock {
        val owner = requireOwner(recordType, lease)
        storeState(recordType, lease, ChangesTokenState(token, ChangesTokenPhase.PENDING_BASELINE))
        storeOwner(recordType, lease.repositoryScopeId, owner.copy(baselineRequired = false))
    }

    suspend fun commitToken(recordType: AnyRecordType, lease: CollectionProjectionLease, token: String) =
        ownerMutex.withLock {
            requireOwner(recordType, lease)
            storeState(recordType, lease, ChangesTokenState(token, ChangesTokenPhase.COMMITTED))
        }

    suspend fun requireProjectionOwner(recordType: AnyRecordType, lease: CollectionProjectionLease) {
        ownerMutex.withLock { requireOwner(recordType, lease) }
    }

    private suspend fun requireOwner(
        recordType: AnyRecordType,
        lease: CollectionProjectionLease,
    ): CollectionProjectionOwner {
        val owner = readOwner(recordType, lease.repositoryScopeId)
        check(owner?.projectionScopeId == lease.projectionScopeId && owner.generation == lease.generation) {
            "This collector no longer owns the repository/type projection lease."
        }
        return owner
    }

    private suspend fun readOwner(type: AnyRecordType, repositoryScopeId: String): CollectionProjectionOwner? =
        storage.read(
            key = ownerKeyFor(type, repositoryScopeId),
            settings = LocalStorageSetting.Unencrypted,
            serializer = serializer(),
        )

    private suspend fun storeOwner(
        type: AnyRecordType,
        repositoryScopeId: String,
        owner: CollectionProjectionOwner,
    ) {
        storage.store(
            key = ownerKeyFor(type, repositoryScopeId),
            value = owner,
            settings = LocalStorageSetting.Unencrypted,
            serializer = serializer(),
        )
    }

    private suspend fun storeState(
        type: AnyRecordType,
        lease: CollectionProjectionLease,
        state: ChangesTokenState,
    ) {
        storage.store(
            key = keyFor(type, lease.repositoryScopeId, lease.projectionScopeId),
            value = state,
            settings = LocalStorageSetting.Unencrypted,
            serializer = serializer(),
        )
    }

    private fun keyFor(type: AnyRecordType, repositoryScopeId: String, collectionScopeId: String): String =
        "health_changes_token_${type.identifier}_${scopeDigest(repositoryScopeId)}_${scopeDigest(collectionScopeId)}"

    private fun ownerKeyFor(type: AnyRecordType, repositoryScopeId: String): String =
        "health_projection_owner_${type.identifier}_${scopeDigest(repositoryScopeId)}"

    private fun scopeDigest(scopeId: String): String {
        require(scopeId.isNotBlank()) { "A collection scope id is required for token storage." }
        return MessageDigest.getInstance("SHA-256")
            .digest(scopeId.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { byte ->
                val unsigned = byte.toInt() and BYTE_MASK
                "${LOWERCASE_HEX[unsigned ushr HEX_NIBBLE_BITS]}${LOWERCASE_HEX[unsigned and HEX_NIBBLE_MASK]}"
            }
    }

    private companion object {
        const val LOWERCASE_HEX = "0123456789abcdef"
        const val HEX_NIBBLE_BITS = 4
        const val HEX_NIBBLE_MASK = 0x0f
        const val BYTE_MASK = 0xff
    }
}

@Serializable
internal data class ChangesTokenState(
    val token: String,
    val phase: ChangesTokenPhase,
) {
    init {
        require(token.isNotBlank()) { "A Health Connect changes token must not be blank." }
    }
}

@Serializable
internal enum class ChangesTokenPhase {
    PENDING_BASELINE,
    COMMITTED,
}

@Serializable
private data class CollectionProjectionOwner(
    val projectionScopeId: String,
    val generation: Long,
    val baselineRequired: Boolean,
) {
    init {
        require(projectionScopeId.isNotBlank() && generation > 0L) {
            "A projection owner requires a scope id and positive generation."
        }
    }

    fun lease(repositoryScopeId: String) = CollectionProjectionLease(
        repositoryScopeId = repositoryScopeId,
        projectionScopeId = projectionScopeId,
        generation = generation,
    )
}

internal data class CollectionProjectionLease(
    val repositoryScopeId: String,
    val projectionScopeId: String,
    val generation: Long,
)
