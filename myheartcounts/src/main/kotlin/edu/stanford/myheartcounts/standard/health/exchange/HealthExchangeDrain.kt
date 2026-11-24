//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("detekt:ReturnCount")

package edu.stanford.myheartcounts.standard.health.exchange

import com.github.luben.zstd.Zstd
import edu.stanford.myheartcounts.firebase.mhcFirebaseLogger
import edu.stanford.myheartcounts.standard.health.HealthUploadCategory
import edu.stanford.myheartcounts.standard.health.ManagedFileUpload
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.fhir.contract.ExchangeGraph
import org.grovealliance.fhir.contract.ExchangeRecord
import java.security.MessageDigest

/**
 * Uploads the staged exchange records of one participant: the converted Records once they cleared
 * the retention window, packed into Bundles per sample type, then the retractions, once every
 * converted Record is uploaded.
 *
 * Each Bundle travels as a file named by its sample type and a digest of its Provenance fullUrls, so
 * the same records always go under the same name. A row that can't be read back, or whose record no
 * Bundle carries, travels alone as an `unreadable_` file of its stored bytes; a row about another
 * participant is dropped. Rows leave staging once every file of their chunk is uploaded.
 */
interface HealthExchangeDrain {

    /**
     * Uploads what of [accountId] is due, the converted Records staged no later than [cutoffMillis],
     * in at most [maximumArchives] files when it is set. Succeeds with whether everything due was uploaded.
     */
    suspend fun drain(accountId: String, cutoffMillis: Long, maximumArchives: Int?): Result<Boolean>
}

/**
 * Default [HealthExchangeDrain].
 */
class HealthExchangeDrainImpl(
    private val records: StagedExchangeRecordDao,
    private val packer: ExchangeChunkPacker,
    private val managedUpload: ManagedFileUpload,
    private val timeProvider: TimeProvider,
) : HealthExchangeDrain {
    private val logger by mhcFirebaseLogger()

    override suspend fun drain(accountId: String, cutoffMillis: Long, maximumArchives: Int?): Result<Boolean> = runCatchingAsync {
        var budget = maximumArchives
        listOf(
            StagedExchangeRecord.KIND_ACTIVE to cutoffMillis,
            StagedExchangeRecord.KIND_RETRACTION to timeProvider.currentTimeMillis(),
        ).all { (kind, cutoff) ->
            records.sampleTypesReady(kind = kind, accountId = accountId, cutoffMillis = cutoff).all { sampleType ->
                var isDrained = false
                while (!isDrained && budget != 0) {
                    val chunk = records.ready(
                        kind = kind,
                        accountId = accountId,
                        sampleTypeIdentifier = sampleType,
                        cutoffMillis = cutoff,
                        limit = CHUNK_SIZE,
                    )
                    if (chunk.isEmpty()) {
                        isDrained = true
                    } else {
                        val uploaded = upload(chunk = chunk, kind = kind, sampleType = sampleType, accountId = accountId).getOrThrow()
                        budget = budget?.minus(uploaded)?.coerceAtLeast(0)
                    }
                }
                isDrained
            }
        }
    }

    /**
     * Uploads one chunk's files and removes its rows; succeeds with how many files it uploaded.
     */
    private suspend fun upload(
        chunk: List<StagedExchangeRecord>,
        kind: String,
        sampleType: String,
        accountId: String,
    ): Result<Int> =
        runCatchingAsync {
            val readable = mutableListOf<Pair<StagedExchangeRecord, ExchangeRecord>>()
            val unreadable = mutableListOf<StagedExchangeRecord>()
            chunk.forEach { row ->
                val record = runCatching { Zstd.decompress(row.payload, Zstd.getFrameContentSize(row.payload).toInt()) }.getOrNull()
                    ?.let { ExchangeRecord.validating(json = it).getOrNull() }
                if (record == null) unreadable += row else readable += row to record
            }
            val packed = packer.pack(records = readable, packedAt = timeProvider.nowInstant())
            val category = if (kind == StagedExchangeRecord.KIND_ACTIVE) HealthUploadCategory.LIVE else HealthUploadCategory.DELETIONS
            packed.bundles.forEach { bundle ->
                managedUpload.upload(
                    accountId = accountId,
                    category = category,
                    fileName = "${sampleType}_${digest(text = provenanceFullUrls(bundle = bundle))}.json.zstd",
                    bytes = Zstd.compress(bundle.json),
                ).getOrThrow()
            }
            (unreadable + packed.failedAlone).forEach { row ->
                managedUpload.upload(
                    accountId = accountId,
                    category = category,
                    fileName = "unreadable_${sampleType}_${digest(bytes = row.payload)}.zstd",
                    bytes = row.payload,
                ).getOrThrow()
            }
            if (packed.otherSubject.isNotEmpty() || unreadable.isNotEmpty() || packed.failedAlone.isNotEmpty()) {
                logger.w {
                    val unreadableCount = unreadable.size + packed.failedAlone.size
                    "$sampleType: $unreadableCount unreadable, ${packed.otherSubject.size} of another participant"
                }
            }
            chunk.forEach { records.remove(kind = it.kind, sampleTypeIdentifier = it.sampleTypeIdentifier, recordId = it.recordId) }
            packed.bundles.size + unreadable.size + packed.failedAlone.size
        }

    private fun provenanceFullUrls(bundle: ExchangeGraph): String =
        bundle.records().getOrThrow().map { it.version.fullUrl }.sorted().joinToString(separator = "\n")

    private fun digest(text: String): String = digest(bytes = text.toByteArray(Charsets.UTF_8))

    /**
     * The first [DIGEST_BYTES] bytes of [bytes]' SHA-256 as lowercase hex, as file names state content.
     */
    private fun digest(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).take(DIGEST_BYTES).joinToString(separator = "") { "%02x".format(it) }

    private companion object {
        const val CHUNK_SIZE = 1_000
        const val DIGEST_BYTES = 12
    }
}
