//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health.exchange

import org.grovealliance.fhir.contract.ExchangeBundlePacker
import org.grovealliance.fhir.contract.ExchangeGraph
import org.grovealliance.fhir.contract.ExchangeRecord
import java.time.Instant

/**
 * Packs one chunk of staged records into as few Bundles as the packer's caps allow.
 *
 * A record that would take a Bundle past a cap starts the next one; one that conflicts with a node
 * the Bundle already states, or is of another kind, starts a fresh Bundle of its own kind. When a
 * Bundle fails to validate, its records are packed one per Bundle, and a record whose one-record
 * Bundle still fails is returned as failed alone. A record about another participant is returned
 * apart and never packed.
 */
interface ExchangeChunkPacker {

    /**
     * The Bundles of [records], keyed by their staging rows, packed at [packedAt].
     */
    fun <K> pack(records: List<Pair<K, ExchangeRecord>>, packedAt: Instant): PackedChunk<K>
}

/**
 * What one chunk became: the [bundles] to upload in order, the rows whose record no Bundle could
 * carry ([failedAlone]), and the rows about another participant ([otherSubject]).
 */
data class PackedChunk<K>(
    val bundles: List<ExchangeGraph>,
    val failedAlone: List<K>,
    val otherSubject: List<K>,
)

/**
 * Default [ExchangeChunkPacker].
 */
class ExchangeChunkPackerImpl : ExchangeChunkPacker {

    override fun <K> pack(records: List<Pair<K, ExchangeRecord>>, packedAt: Instant): PackedChunk<K> {
        val bundles = mutableListOf<ExchangeGraph>()
        val failedAlone = mutableListOf<K>()
        val otherSubject = mutableListOf<K>()
        var packer: ExchangeBundlePacker? = null
        var packed = mutableListOf<Pair<K, ExchangeRecord>>()

        fun close() {
            val current = packer ?: return
            if (packed.isEmpty()) return
            current.bundle(packedAt = packedAt).fold(
                onSuccess = { bundles += it },
                onFailure = { packed.forEach { entry -> packAlone(
                    entry = entry,
                    packedAt = packedAt,
                    bundles = bundles,
                    failedAlone = failedAlone,
                ) } },
            )
            packed = mutableListOf()
        }

        fun start(entry: Pair<K, ExchangeRecord>) {
            close()
            val fresh = ExchangeBundlePacker(kind = entry.second.kind)
            packer = fresh
            if (fresh.insert(record = entry.second).isSuccess) packed += entry else failedAlone += entry.first
        }

        records.forEach { entry ->
            val current = packer
            if (current == null) {
                start(entry = entry)
                return@forEach
            }
            current.insert(record = entry.second).fold(
                onSuccess = { result ->
                    when (result) {
                        ExchangeBundlePacker.InsertResult.INSERTED -> packed += entry
                        ExchangeBundlePacker.InsertResult.DUPLICATE -> Unit
                        ExchangeBundlePacker.InsertResult.DEFERRED -> start(entry = entry)
                    }
                },
                onFailure = { failure ->
                    when (failure) {
                        is ExchangeBundlePacker.Failure.MixedSubjects -> otherSubject += entry.first
                        else -> start(entry = entry)
                    }
                },
            )
        }
        close()
        return PackedChunk(
            bundles = bundles,
            failedAlone = failedAlone,
            otherSubject = otherSubject,
        )
    }

    private fun <K> packAlone(
        entry: Pair<K, ExchangeRecord>,
        packedAt: Instant,
        bundles: MutableList<ExchangeGraph>,
        failedAlone: MutableList<K>,
    ) {
        val alone = ExchangeBundlePacker(kind = entry.second.kind)
        val bundle = alone.insert(record = entry.second).fold(
            onSuccess = { alone.bundle(packedAt = packedAt).getOrNull() },
            onFailure = { null },
        )
        if (bundle != null) bundles += bundle else failedAlone += entry.first
    }
}
