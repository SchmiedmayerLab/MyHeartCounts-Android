//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health

import androidx.health.connect.client.records.Record
import ca.uhn.fhir.context.FhirContext
import com.github.luben.zstd.Zstd
import com.google.firebase.firestore.FirebaseFirestore
import edu.stanford.myheartcounts.firebase.MHCFirestore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import org.grovealliance.core.Module
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.health.RecordType
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Period
import java.util.UUID

/**
 * Delivers Health Connect data to the backend.
 *
 * [MHCStandard][edu.stanford.myheartcounts.standard.MHCStandard] receives the records from Grove and
 * hands them here; this decides how each sample type travels ([HealthSampleType]), turns records into
 * FHIR (`HealthObservationMapper`), and either stages them for a later bulk upload or sends them
 * straight out. Ports `MyHeartCountsStandard+HealthKit`.
 */
class MHCHealthDataHandler(
    private val staging: HealthStagingDatabase,
    private val mapper: HealthObservationMapper,
    private val managedUpload: ManagedFileUpload,
    private val firestore: MHCFirestore,
    private val timeProvider: TimeProvider,
    private val provenance: suspend () -> HealthObservationProvenance,
) : Module {

    private val logger by groveLogger(tag = "MHCFirebase")
    private val fhirParser by lazy { FhirContext.forR4().newJsonParser() }

    /**
     * Serializes upload runs. Signing in both recovers interrupted uploads and starts the upload
     * worker, whose first run is immediate; two runs reading the same staged rows would each upload
     * them.
     */
    private val uploadLock = Mutex()

    /**
     * Converts [records] to FHIR and routes them by the strategy their sample type calls for.
     */
    suspend fun <T : Record> handleNewRecords(records: Set<T>, type: RecordType<out T>) {
        if (records.isEmpty()) return
        val sampleType = HealthSampleType.of(recordType = type)
        val issuedAt = timeProvider.nowInstant()
        // Resolved once per batch rather than per observation: it is the same for all of them, and
        // reading it involves a database round trip.
        val provenance = provenance()
        val observationsByRecord = records.associateWith { record ->
            mapper.map(record = record, sampleType = sampleType, issuedAt = issuedAt)
                .onEach { provenance.applyTo(observation = it) }
        }
        val observations = observationsByRecord.values.flatten()
        if (observations.isEmpty()) return
        trackSeriesReadings(sampleType = sampleType, observationsByRecord = observationsByRecord)

        when (sampleType.strategy) {
            HealthUploadStrategy.QUEUE_LOCALLY -> stage(
                sampleType = sampleType,
                observations = observations,
            )
            HealthUploadStrategy.FIREBASE_STORAGE -> uploadArchiveJson(
                accountId = firestore.accountId,
                sampleTypeIdentifier = sampleType.identifier,
                json = observations.joinToString(separator = ",", prefix = "[", postfix = "]") {
                    fhirParser.encodeResourceToString(it)
                },
                category = HealthUploadCategory.LIVE,
            )
            HealthUploadStrategy.DIRECT_FIRESTORE -> uploadToFirestore(
                sampleType = sampleType,
                observations = observations,
            )
        }
    }

    /**
     * Drops what is still staged for the deleted records, and stages deletions for whatever of them
     * already reached the backend.
     */
    suspend fun <T : Record> handleDeletedRecords(recordIds: Set<String>, type: RecordType<out T>) {
        if (recordIds.isEmpty()) return
        val sampleType = HealthSampleType.of(recordType = type)
        val accountId = firestore.accountId
        val deletedAt = timeProvider.currentTimeMillis()
        val sampleIds = recordIds.flatMap { recordId -> sampleIdsToReport(recordId = recordId, sampleType = sampleType) }
        if (sampleIds.isEmpty()) return
        staging.deletions().stageDeletions(
            deletions = sampleIds.map { sampleId ->
                StagedDeletion(
                    sampleId = sampleId,
                    sampleTypeIdentifier = sampleType.identifier,
                    accountId = accountId,
                    deletedAtMillis = deletedAt,
                )
            },
        )
    }

    /**
     * Drops the staged rows for [type], because Health Connect's change token expired and what is
     * staged may no longer match what it holds; the next collection re-reads that type in full.
     *
     * This has no iOS counterpart — HealthKit anchors do not expire the way Health Connect tokens
     * do.
     */
    suspend fun <T : Record> onFullyResyncRequired(type: RecordType<out T>) {
        val sampleType = HealthSampleType.of(recordType = type)
        logger.i { "Re-syncing '${sampleType.identifier}'; dropping what was staged for it." }
        staging.observations().removeObservationsOfType(sampleTypeIdentifier = sampleType.identifier)
    }

    /**
     * Uploads everything staged that has cleared the retention window, plus any deletions.
     *
     * Called from [HealthUploadWorker]; safe to call at any time and a no-op when nothing is due.
     */
    suspend fun uploadStagedData(): Result<Unit> = uploadLock.withLock { uploadStagedDataLocked() }

    private suspend fun uploadStagedDataLocked(): Result<Unit> = runCatching {
        firestore.awaitReady()
        // Read once, so a participant signing in while this runs cannot have anyone else's rows
        // uploaded as theirs.
        val accountId = firestore.accountId
        val now = timeProvider.currentTimeMillis()
        val cutoff = now - HealthUploadSchedule.RETENTION_OFFSET_DAYS * MILLIS_PER_DAY

        staging.observations().sampleTypesReadyForUpload(accountId = accountId, cutoffMillis = cutoff)
            .forEach { identifier ->
                uploadStagedType(accountId = accountId, sampleTypeIdentifier = identifier, cutoffMillis = cutoff)
            }
        uploadStagedDeletions(accountId = accountId)
        staging.seriesReadings().removeOlderThan(
            cutoffMillis = now - HealthUploadSchedule.SERIES_READINGS_RETENTION_DAYS * MILLIS_PER_DAY,
        )
    }

    /**
     * Re-uploads archives an interrupted session left on disk, after dropping whatever another
     * participant left staged.
     */
    suspend fun recoverInterruptedUploads() {
        uploadLock.withLock {
            firestore.awaitReady()
            val accountId = firestore.accountId
            staging.observations().removeObservationsNotOwnedBy(accountId = accountId)
            staging.deletions().removeDeletionsNotOwnedBy(accountId = accountId)
            managedUpload.recoverOrphans()
        }
    }

    /**
     * Drops everything waiting to be uploaded, for when the participant signs out.
     */
    suspend fun clearPendingUploads() {
        staging.observations().clearObservations()
        staging.deletions().clearDeletions()
        staging.seriesReadings().clear()
        managedUpload.discardStaged()
    }

    /**
     * Remembers which observations each record carrying a series produced, merged with what earlier
     * deliveries of the same record produced, so that deleting the record can name every reading.
     */
    private suspend fun trackSeriesReadings(
        sampleType: HealthSampleType,
        observationsByRecord: Map<out Record, List<Observation>>,
    ) {
        val series = staging.seriesReadings()
        val now = timeProvider.currentTimeMillis()
        val readings = observationsByRecord.mapNotNull { (record, observations) ->
            val recordId = record.metadata.id.ifEmpty { null } ?: return@mapNotNull null
            val readingIds = observations.map { it.id }.filter { it != recordId }
            if (readingIds.isEmpty()) return@mapNotNull null
            val known = series.readings(recordId = recordId, sampleTypeIdentifier = sampleType.identifier)
                ?.observationIds
                ?.split(SERIES_ID_SEPARATOR)
                .orEmpty()
            SeriesRecordReadings(
                recordId = recordId,
                sampleTypeIdentifier = sampleType.identifier,
                observationIds = (known + readingIds).distinct().joinToString(separator = SERIES_ID_SEPARATOR),
                updatedAtMillis = now,
            )
        }
        if (readings.isNotEmpty()) series.save(readings = readings)
    }

    /**
     * The sample ids the backend has to retract for the deleted [recordId], after dropping whatever
     * of the record had not left the device yet.
     *
     * Matches iOS, which reports nothing for a sample it still held: the backend never received it.
     */
    private suspend fun sampleIdsToReport(recordId: String, sampleType: HealthSampleType): List<String> {
        val series = staging.seriesReadings()
        val readingIds = series.readings(recordId = recordId, sampleTypeIdentifier = sampleType.identifier)
            ?.observationIds
            ?.split(SERIES_ID_SEPARATOR)
        if (readingIds == null) {
            val removed = staging.observations().removeObservationsOfRecord(recordId = recordId)
            return if (removed > 0) emptyList() else listOf(recordId)
        }
        val stillStaged = series.stagedObservationIdsAmong(ids = readingIds).toSet()
        staging.observations().removeObservations(ids = stillStaged.toList())
        series.remove(recordId = recordId, sampleTypeIdentifier = sampleType.identifier)
        return readingIds.filterNot { it in stillStaged }
    }

    private suspend fun stage(sampleType: HealthSampleType, observations: List<Observation>) {
        val accountId = firestore.accountId
        staging.observations().stage(
            observations = observations.map { observation ->
                StagedObservation(
                    id = observation.id,
                    sampleTypeIdentifier = sampleType.identifier,
                    accountId = accountId,
                    json = fhirParser.encodeResourceToString(observation),
                    effectiveAtMillis = observation.effectiveEndMillis() ?: timeProvider.currentTimeMillis(),
                )
            },
        )
    }

    private suspend fun uploadStagedType(accountId: String, sampleTypeIdentifier: String, cutoffMillis: Long) {
        while (true) {
            val batch = staging.observations().observationsReadyForUpload(
                accountId = accountId,
                sampleTypeIdentifier = sampleTypeIdentifier,
                cutoffMillis = cutoffMillis,
                limit = HealthUploadSchedule.ARCHIVE_BATCH_SIZE,
            )
            if (batch.isEmpty()) return

            val archived = uploadArchiveJson(
                accountId = accountId,
                sampleTypeIdentifier = sampleTypeIdentifier,
                json = batch.joinToString(separator = ",", prefix = "[", postfix = "]") { it.json },
                category = HealthUploadCategory.LIVE,
            )
            // Leaving the rows staged on failure is what makes an upload retryable; dropping them
            // here would lose the samples for good.
            if (archived.isFailure) return
            staging.observations().removeObservations(ids = batch.map { it.id })
        }
    }

    /**
     * Uploads the staged deletions as one archive per sample type, which is the shape iOS uploads
     * and the backend ingests.
     */
    private suspend fun uploadStagedDeletions(accountId: String) {
        staging.deletions().allDeletions(accountId = accountId)
            .groupBy { it.sampleTypeIdentifier }
            .forEach { (sampleTypeIdentifier, deletions) ->
                val csv = buildString {
                    appendLine(DELETIONS_CSV_HEADER)
                    deletions.forEach { deletion ->
                        appendLine("${deletion.sampleTypeIdentifier},${deletion.sampleId},${deletion.deletedAtMillis}")
                    }
                }
                val uploaded = managedUpload.upload(
                    accountId = accountId,
                    category = HealthUploadCategory.DELETIONS,
                    fileName = "${sampleTypeIdentifier}_${UUID.randomUUID()}.csv.zstd",
                    bytes = Zstd.compress(csv.toByteArray()),
                )
                // Left staged on failure, so the next run reports them again.
                if (uploaded.isSuccess) {
                    staging.deletions().removeDeletions(sampleIds = deletions.map { it.sampleId })
                }
            }
    }

    private suspend fun uploadArchiveJson(
        accountId: String,
        sampleTypeIdentifier: String,
        json: String,
        category: HealthUploadCategory,
    ): Result<Unit> = managedUpload.upload(
        accountId = accountId,
        category = category,
        // The backend splits this name on the underscore to recover the sample type
        // (`onArchivedLiveHealthSampleUploaded.test.ts:91`).
        fileName = "${sampleTypeIdentifier}_${UUID.randomUUID()}.json.zstd",
        bytes = Zstd.compress(json.toByteArray()),
    )

    private suspend fun uploadToFirestore(sampleType: HealthSampleType, observations: List<Observation>) {
        runCatching {
            firestore.awaitReady()
            val collection = firestore.healthObservations(sampleTypeIdentifier = sampleType.identifier)
            observations.chunked(HealthUploadSchedule.FIRESTORE_BATCH_SIZE).forEach { chunk ->
                val batch = FirebaseFirestore.getInstance().batch()
                chunk.forEach { observation ->
                    batch.set(
                        collection.document(observation.id),
                        FhirJson.toFirestoreMap(json = fhirParser.encodeResourceToString(observation)),
                    )
                }
                batch.commit().await()
            }
        }.onFailure { throwable ->
            logger.e(throwable) { "Failed to write '${sampleType.identifier}' observations to Firestore." }
        }
    }

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

        /**
         * The columns the backend's deletion ingestion expects.
         */
        const val DELETIONS_CSV_HEADER = "sampleType,sampleId,timestamp"

        /**
         * Joins the observation ids tracked for one series record. Observation ids never contain it.
         */
        const val SERIES_ID_SEPARATOR = ","
    }
}

/**
 * When staged health data is uploaded, and in what size.
 */
object HealthUploadSchedule {

    /**
     * How long a sample is held on device before being uploaded.
     *
     * Matches iOS's `dataRetentionOffsetInDays`. The delay is what gives a participant a window to
     * delete a sample from Health Connect before it ever leaves the device.
     */
    const val RETENTION_OFFSET_DAYS = 3L

    /**
     * How long the readings a series record produced are remembered after its last delivery.
     *
     * Deleting a record older than this reports only the record's own id, which does not match the
     * readings the backend holds. A year bounds the table while covering the deletions participants
     * realistically make.
     */
    const val SERIES_READINGS_RETENTION_DAYS = 365L

    /**
     * How often the uploader runs. iOS schedules its equivalent background task six hours out.
     */
    const val UPLOAD_INTERVAL_HOURS = 6L

    /**
     * How many staged observations go into one archive.
     */
    const val ARCHIVE_BATCH_SIZE = 1_000

    /**
     * How many observations are written to Firestore at a time, matching iOS's batch size.
     */
    const val FIRESTORE_BATCH_SIZE = 100
}

/**
 * When the measurement this observation describes finished, or `null` when it carries no time.
 *
 * This is what the retention window is applied to, and it is read per observation rather than per
 * batch, off the measurement's own time rather than off when Health Connect last wrote the row.
 * Either shortcut would let a sample recorded minutes ago inherit an older one's age and be
 * uploaded straight away, defeating the window that gives a participant a few days to delete it
 * first.
 *
 * A measurement spanning an interval is judged by its end, so nothing leaves the device before it
 * is genuinely [HealthUploadSchedule.RETENTION_OFFSET_DAYS] old.
 */
internal fun Observation.effectiveEndMillis(): Long? = when (val effective = effective) {
    is DateTimeType -> effective.value?.time
    is Period -> effective.end?.time ?: effective.start?.time
    else -> null
}
