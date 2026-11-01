//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import edu.stanford.myheartcounts.firebase.FirebaseSchema.StatsDocument

/**
 * Reads [HealthStatsDocument]s from their stored field values.
 *
 * A document whose structure is invalid is rejected as a whole; an entry that cannot be read is
 * left out and counted in [HealthStatsDocument.malformedEntryCount], so that one bad entry does not
 * cost the rest of the month. Fields the reader does not know are ignored.
 */
interface HealthStatsDocumentDecoder {

    /**
     * The document [data] describes, or a [HealthStatsDecodingException] when its structure is
     * invalid.
     */
    fun decode(data: Map<String, Any?>): Result<HealthStatsDocument>
}

class HealthStatsDocumentDecoderImpl(
    private val entryDecoder: HealthStatsEntryDecoder,
) : HealthStatsDocumentDecoder {

    @Suppress("detekt:ReturnCount")
    override fun decode(data: Map<String, Any?>): Result<HealthStatsDocument> {
        val version = version(data[StatsDocument.VERSION]) ?: return failure(message = "${StatsDocument.VERSION} is not an integer")
        val metric = data[StatsDocument.METRIC] as? String ?: return failure(message = "${StatsDocument.METRIC} is not a string")
        val collectionKey = ENTRY_COLLECTION_KEYS.filter { it in data }.singleOrNull()
            ?: return failure(message = "Expected exactly one stats entry kind")
        val collection = data[collectionKey] as? Map<*, *> ?: return failure(message = "Entries are not keyed by source")

        var malformedEntryCount = 0
        val entriesBySourceId = mutableMapOf<String, List<HealthStatsEntry>>()
        for ((source, rawEntries) in collection) {
            val sourceId = source as? String ?: return failure(message = "Source identifier is not a string")
            val entries = rawEntries as? List<*> ?: return failure(message = "Entries of $sourceId are not a list")
            entriesBySourceId[sourceId] = entries.mapNotNull { rawEntry ->
                entryDecoder.decode(raw = rawEntry).also { if (it == null) malformedEntryCount++ }
            }
        }
        return Result.success(
            HealthStatsDocument(
                version = version,
                metric = metric,
                entriesBySourceId = entriesBySourceId,
                malformedEntryCount = malformedEntryCount,
            )
        )
    }

    private fun version(value: Any?): Int? = when (value) {
        is Long -> value.toInt()
        is Int -> value
        is Double -> value.takeIf { it.isFinite() && it == Math.rint(it) }?.toInt()
        else -> null
    }

    private fun failure(message: String): Result<HealthStatsDocument> =
        Result.failure(HealthStatsDecodingException(message = message))

    private companion object {
        val ENTRY_COLLECTION_KEYS = listOf(
            StatsDocument.HOURLY,
            StatsDocument.DAILY,
            StatsDocument.SESSIONS,
            StatsDocument.SAMPLES,
        )
    }
}

/**
 * Describes why a statistics document could not be read.
 */
class HealthStatsDecodingException(message: String) : Exception(message)
