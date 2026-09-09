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
     * Converts [records] to FHIR and routes them by the strategy their sample type calls for.
     */
    suspend fun <T : Record> handleNewRecords(records: Set<T>, type: RecordType<out T>) {
        if (records.isEmpty()) return
        val sampleType = HealthSampleType.of(recordType = type)
        val issuedAt = timeProvider.nowInstant()
        // Resolved once per batch rather than per observation: it is the same for all of them, and
        // reading it involves a database round trip.
        val provenance = provenance()
        val observations = records.flatMap { record ->
            mapper.map(record = record, sampleType = sampleType, issuedAt = issuedAt)
                .onEach { provenance.applyTo(observation = it) }
        }
        if (observations.isEmpty()) return

        when (sampleType.strategy) {
            HealthUploadStrategy.QUEUE_LOCALLY -> stage(
                sampleType = sampleType,
                observations = observations,
            )
            HealthUploadStrategy.FIREBASE_STORAGE -> uploadArchive(
                sampleType = sampleType,
                observations = observations,
                category = HealthUploadCategory.LIVE,
            )
            HealthUploadStrategy.DIRECT_FIRESTORE -> uploadToFirestore(
                sampleType = sampleType,
                observations = observations,
            )
        }
    }

    /**
     * Stages the deletions so the backend can retract the samples it already holds.
     */
    suspend fun <T : Record> handleDeletedRecords(recordIds: Set<String>, type: RecordType<out T>) {
        if (recordIds.isEmpty()) return
        val sampleType = HealthSampleType.of(recordType = type)
        val deletedAt = timeProvider.currentTimeMillis()
        staging.dao().stageDeletions(
            deletions = recordIds.map { recordId ->
                StagedDeletion(
                    sampleId = recordId,
                    sampleTypeIdentifier = sampleType.identifier,
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
        staging.dao().removeObservationsOfType(sampleTypeIdentifier = sampleType.identifier)
    }

    /**
     * Uploads everything staged that has cleared the retention window, plus any deletions.
     *
     * Called from [HealthUploadWorker]; safe to call at any time and a no-op when nothing is due.
     */
    suspend fun uploadStagedData(): Result<Unit> = runCatching {
        firestore.awaitReady()
        val cutoff = timeProvider.nowInstant()
            .minusMillis(HealthUploadSchedule.RETENTION_OFFSET_DAYS * MILLIS_PER_DAY)
            .toEpochMilli()

        staging.dao().sampleTypesReadyForUpload(cutoffMillis = cutoff).forEach { identifier ->
            uploadStagedType(sampleTypeIdentifier = identifier, cutoffMillis = cutoff)
        }
        uploadStagedDeletions()
    }

    /**
     * Re-uploads archives an interrupted session left on disk.
     */
    suspend fun recoverInterruptedUploads() {
        firestore.awaitReady()
        managedUpload.recoverOrphans()
    }

    /**
     * Drops everything waiting to be uploaded, for when the participant signs out.
     */
    suspend fun clearPendingUploads() {
        staging.dao().clearObservations()
        staging.dao().clearDeletions()
        managedUpload.discardStaged()
    }

    private suspend fun stage(sampleType: HealthSampleType, observations: List<Observation>) {
        staging.dao().stage(
            observations = observations.map { observation ->
                StagedObservation(
                    id = observation.id,
                    sampleTypeIdentifier = sampleType.identifier,
                    json = fhirParser.encodeResourceToString(observation),
                    effectiveAtMillis = effectiveMillis(observation = observation),
                )
            },
        )
    }

    private suspend fun uploadStagedType(sampleTypeIdentifier: String, cutoffMillis: Long) {
        while (true) {
            val batch = staging.dao().observationsReadyForUpload(
                sampleTypeIdentifier = sampleTypeIdentifier,
                cutoffMillis = cutoffMillis,
                limit = HealthUploadSchedule.ARCHIVE_BATCH_SIZE,
            )
            if (batch.isEmpty()) return

            val archived = uploadArchiveJson(
                sampleTypeIdentifier = sampleTypeIdentifier,
                json = batch.joinToString(separator = ",", prefix = "[", postfix = "]") { it.json },
                category = HealthUploadCategory.LIVE,
            )
            // Leaving the rows staged on failure is what makes an upload retryable; dropping them
            // here would lose the samples for good.
            if (archived.isFailure) return
            staging.dao().removeObservations(ids = batch.map { it.id })
        }
    }

    private suspend fun uploadStagedDeletions() {
        val deletions = staging.dao().allDeletions()
        if (deletions.isEmpty()) return

        val csv = buildString {
            appendLine(DELETIONS_CSV_HEADER)
            deletions.forEach { deletion ->
                appendLine("${deletion.sampleTypeIdentifier},${deletion.sampleId},${deletion.deletedAtMillis}")
            }
        }
        val uploaded = managedUpload.upload(
            category = HealthUploadCategory.DELETIONS,
            fileName = "deletions_${UUID.randomUUID()}.csv.zstd",
            bytes = Zstd.compress(csv.toByteArray()),
        )
        if (uploaded.isFailure) return

        // The backend also drains a Firestore queue of deletions, which is what the deletion service
        // replays against the collections the samples were uploaded to.
        runCatching {
            deletions.forEach { deletion ->
                firestore.pendingHealthSampleDeletions
                    .document(deletion.sampleId)
                    .set(
                        mapOf(
                            "collectionName" to "HealthObservations_${deletion.sampleTypeIdentifier}",
                            "sampleId" to deletion.sampleId,
                        ),
                    )
                    .await()
            }
        }.onFailure { throwable ->
            logger.e(throwable) { "Failed to enqueue health sample deletions in Firestore." }
            return
        }
        staging.dao().removeDeletions(sampleIds = deletions.map { it.sampleId })
    }

    private suspend fun uploadArchive(
        sampleType: HealthSampleType,
        observations: List<Observation>,
        category: HealthUploadCategory,
    ) {
        uploadArchiveJson(
            sampleTypeIdentifier = sampleType.identifier,
            json = observations.joinToString(separator = ",", prefix = "[", postfix = "]") {
                fhirParser.encodeResourceToString(it)
            },
            category = category,
        )
    }

    private suspend fun uploadArchiveJson(
        sampleTypeIdentifier: String,
        json: String,
        category: HealthUploadCategory,
    ): Result<Unit> = managedUpload.upload(
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

    private fun effectiveMillis(observation: Observation): Long =
        observation.effectiveEndMillis() ?: timeProvider.currentTimeMillis()

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

        /**
         * The columns the backend's deletion ingestion expects.
         */
        const val DELETIONS_CSV_HEADER = "sampleType,sampleId,timestamp"
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
