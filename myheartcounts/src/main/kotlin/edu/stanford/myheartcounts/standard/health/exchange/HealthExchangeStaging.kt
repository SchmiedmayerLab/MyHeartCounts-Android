//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health.exchange

import androidx.health.connect.client.records.Record
import com.github.luben.zstd.Zstd
import edu.stanford.myheartcounts.firebase.mhcFirebaseLogger
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.health.fhir.HealthConnectDeletion
import org.grovealliance.health.fhir.HealthConnectExport
import org.grovealliance.health.fhir.HealthConnectRetraction
import kotlin.reflect.KClass

/**
 * Converts Health Connect Records and deletions into exchange records and stages them for
 * [HealthExchangeDrain]: a converted Record as itself, a deleted one as its retraction.
 */
interface HealthExchangeStaging {

    /**
     * Converts [records] of [sampleTypeIdentifier] for [accountId] and stages every record they
     * convert to; a refused Record is logged and left out.
     */
    suspend fun stageRecords(records: List<Record>, sampleTypeIdentifier: String, accountId: String): Result<Unit>

    /**
     * Drops what is staged for the Records with [recordIds] of [recordType], and stages their retractions.
     */
    suspend fun stageDeletions(
        recordIds: Set<String>,
        recordType: KClass<out Record>,
        sampleTypeIdentifier: String,
        accountId: String,
    ): Result<Unit>
}

/**
 * Default [HealthExchangeStaging], converting through [exporterProvider] into [records].
 */
class HealthExchangeStagingImpl(
    private val records: StagedExchangeRecordDao,
    private val exporterProvider: FhirExchangeExporterProvider,
    private val timeProvider: TimeProvider,
) : HealthExchangeStaging {
    private val logger by mhcFirebaseLogger()

    override suspend fun stageRecords(records: List<Record>, sampleTypeIdentifier: String, accountId: String): Result<Unit> =
        runCatchingAsync {
            if (records.isEmpty()) return@runCatchingAsync
            val exporter = exporterProvider.exporter(accountId = accountId).getOrThrow()
            val now = timeProvider.nowInstant()
            val exports = exporter.export(records = records, instant = now)
            exports.filter { it.outcome is HealthConnectExport.Outcome.Refused }.forEach { refused ->
                val diagnostic = (refused.outcome as HealthConnectExport.Outcome.Refused).error.diagnostic
                logger.w { "Refused a ${refused.source.recordType}: ${diagnostic.code} at ${diagnostic.location}" }
            }
            this.records.stage(
                records = exports.mapNotNull { export ->
                    export.record?.let { record ->
                        StagedExchangeRecord(
                            kind = StagedExchangeRecord.KIND_ACTIVE,
                            sampleTypeIdentifier = sampleTypeIdentifier,
                            recordId = export.source.recordId,
                            accountId = accountId,
                            payload = Zstd.compress(record.json),
                            stagedAtMillis = now.toEpochMilli(),
                        )
                    }
                },
            )
        }

    override suspend fun stageDeletions(
        recordIds: Set<String>,
        recordType: KClass<out Record>,
        sampleTypeIdentifier: String,
        accountId: String,
    ): Result<Unit> = runCatchingAsync {
        if (recordIds.isEmpty()) return@runCatchingAsync
        val exporter = exporterProvider.exporter(accountId = accountId).getOrThrow()
        val now = timeProvider.nowInstant()
        recordIds.forEach { records.remove(
            kind = StagedExchangeRecord.KIND_ACTIVE,
            sampleTypeIdentifier = sampleTypeIdentifier,
            recordId = it,
        ) }
        val retractions = exporter.retract(
            deletions = recordIds.map { recordId ->
                HealthConnectDeletion(
                    recordId = recordId,
                    recordType = recordType,
                    deletedAfter = null,
                    detectedAt = now,
                )
            },
            instant = now,
        )
        retractions.filter { it.outcome is HealthConnectRetraction.Outcome.Refused }.forEach { refused ->
            val diagnostic = (refused.outcome as HealthConnectRetraction.Outcome.Refused).error.diagnostic
            logger.w { "Refused to retract a $sampleTypeIdentifier: ${diagnostic.code} at ${diagnostic.location}" }
        }
        records.stage(
            records = retractions.mapNotNull { retraction ->
                retraction.record?.let { record ->
                    StagedExchangeRecord(
                        kind = StagedExchangeRecord.KIND_RETRACTION,
                        sampleTypeIdentifier = sampleTypeIdentifier,
                        recordId = retraction.deletion.recordId,
                        accountId = accountId,
                        payload = Zstd.compress(record.json),
                        stagedAtMillis = now.toEpochMilli(),
                    )
                }
            },
        )
    }
}
