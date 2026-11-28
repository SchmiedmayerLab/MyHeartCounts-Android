//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.testing.populatedWithTestValues
import androidx.health.connect.client.units.Pressure
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.luben.zstd.Zstd
import com.google.common.truth.Truth.assertThat
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import edu.stanford.myheartcounts.standard.health.HealthSampleType
import edu.stanford.myheartcounts.standard.health.HealthStagingDatabase
import edu.stanford.myheartcounts.standard.health.exchange.HealthExchangePipeline
import edu.stanford.myheartcounts.standard.health.exchange.StagedExchangeRecord
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.grovealliance.core.requireDependency
import org.grovealliance.fhir.contract.ExchangeGraph
import org.grovealliance.fhir.contract.ExchangeRecord
import org.grovealliance.health.RecordType
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneOffset

/**
 * Exercises the record-centric upload path against Cloud Storage: Records converted and staged as
 * exchange records, packed into Bundles once due, and deletions sent as retractions.
 *
 * Records are built with the metadata Health Connect assigns rather than read from it, so the test
 * needs no health data or permissions on the device; collection itself is covered by Grove.
 */
@RunWith(AndroidJUnit4::class)
class FhirExchangeUploadTest {

    private val pipeline: HealthExchangePipeline get() = requireDependency()
    private val staging: HealthStagingDatabase get() = requireDependency()
    private val sampleType = HealthSampleType.of(recordType = RecordType.bloodPressure).identifier

    @Before
    fun signIn() = runTest {
        assumeTrue("Needs -Pmhc.firebaseEmulatorHost", FirebaseTestSession.isEmulatorConfigured)
        FirebaseTestSession.initializeFirebase()
        FirebaseTestSession.signUpFreshParticipant()
        staging.exchangeRecords().clear()
    }

    @After
    fun signOut() = runTest {
        staging.exchangeRecords().clear()
        FirebaseTestSession.signOut()
    }

    @Test
    fun holdsFreshRecordsBackAndUploadsDueOnesAsOneBundle() = runTest {
        // given
        val accountId = FirebaseTestSession.firestore.accountId()
        stage(ids = listOf("record-1", "record-2"))
        val stagedAt = staged(kind = StagedExchangeRecord.KIND_ACTIVE).maxOf { it.stagedAtMillis }
        pipeline.drain.drain(accountId = accountId, cutoffMillis = stagedAt - 1, maximumArchives = null).getOrThrow()
        assertThat(archivesIn(folder = LIVE_FOLDER)).isEmpty()

        // when
        val isComplete = pipeline.drain.drain(accountId = accountId, cutoffMillis = stagedAt, maximumArchives = null).getOrThrow()

        // then
        val archive = archivesIn(folder = LIVE_FOLDER).single()
        val bundle = bundle(archive = archive)
        assertThat(isComplete).isTrue()
        assertThat(archive.name).matches("${sampleType}_[0-9a-f]{24}\\.json\\.zstd")
        assertThat(ExchangeGraph.validating(json = bundle, kind = ExchangeRecord.Kind.ACTIVE).getOrThrow().records().getOrThrow())
            .hasSize(2)
        assertThat(count(bundle = bundle, resourceType = "Observation")).isEqualTo(2)
        assertThat(count(bundle = bundle, resourceType = "ResearchStudy")).isAtMost(1)
        assertThat(staged(kind = StagedExchangeRecord.KIND_ACTIVE)).isEmpty()
    }

    @Test
    fun uploadsDeletedRecordsAsOneRetractionBundle() = runTest {
        // given
        val accountId = FirebaseTestSession.firestore.accountId()
        val ids = listOf("record-1", "record-2", "record-3")
        stage(ids = ids)
        val sourceRecords = staged(kind = StagedExchangeRecord.KIND_ACTIVE).map { record(row = it).sourceRecord }.toSet()
        pipeline.staging.stageDeletions(
            recordIds = ids.toSet(),
            recordType = BloodPressureRecord::class,
            sampleTypeIdentifier = sampleType,
            accountId = accountId,
        ).getOrThrow()

        // when
        pipeline.drain.drain(accountId = accountId, cutoffMillis = Long.MAX_VALUE, maximumArchives = null).getOrThrow()

        // then
        val bundle = bundle(archive = archivesIn(folder = DELETIONS_FOLDER).single())
        val retractions = ExchangeGraph.validating(json = bundle, kind = ExchangeRecord.Kind.RETRACTION).getOrThrow().records().getOrThrow()
        assertThat(retractions.map { it.sourceRecord }.toSet()).isEqualTo(sourceRecords)
        assertThat(count(bundle = bundle, resourceType = "Observation")).isEqualTo(0)
        assertThat(archivesIn(folder = LIVE_FOLDER)).isEmpty()
        assertThat(staged(kind = StagedExchangeRecord.KIND_RETRACTION)).isEmpty()
    }

    private suspend fun stage(ids: List<String>) {
        pipeline.staging.stageRecords(
            records = ids.mapIndexed { index, id -> bloodPressure(id = id, systolic = 118.0 + index) },
            sampleTypeIdentifier = sampleType,
            accountId = FirebaseTestSession.firestore.accountId(),
        ).getOrThrow()
    }

    private suspend fun staged(kind: String): List<StagedExchangeRecord> = staging.exchangeRecords().ready(
        kind = kind,
        accountId = FirebaseTestSession.firestore.accountId(),
        sampleTypeIdentifier = sampleType,
        cutoffMillis = Long.MAX_VALUE,
        limit = STAGED_LIMIT,
    )

    private fun record(row: StagedExchangeRecord): ExchangeRecord =
        ExchangeRecord.validating(json = decompress(bytes = row.payload)).getOrThrow()

    private suspend fun archivesIn(folder: String): List<StorageReference> = FirebaseStorage.getInstance().reference
        .child("users/${FirebaseTestSession.firestore.accountId()}/$folder")
        .listAll()
        .await()
        .items

    private suspend fun bundle(archive: StorageReference): ByteArray = decompress(bytes = archive.getBytes(MAX_ARCHIVE_BYTES).await())

    private fun decompress(bytes: ByteArray): ByteArray = Zstd.decompress(bytes, Zstd.getFrameContentSize(bytes).toInt())

    private fun count(bundle: ByteArray, resourceType: String): Int =
        bundle.decodeToString().split("\"resourceType\":\"$resourceType\"").size - 1

    private fun bloodPressure(id: String, systolic: Double) = BloodPressureRecord(
        time = Instant.now().minusSeconds(RECORD_AGE_SECONDS),
        zoneOffset = ZoneOffset.UTC,
        systolic = Pressure.millimetersOfMercury(systolic),
        diastolic = Pressure.millimetersOfMercury(DIASTOLIC),
        metadata = Metadata.manualEntry().populatedWithTestValues(
            id = id,
            dataOrigin = DataOrigin(packageName = "edu.stanford.myheartcounts"),
            lastModifiedTime = Instant.now().minusSeconds(RECORD_AGE_SECONDS),
        ),
    )

    private companion object {
        const val LIVE_FOLDER = "liveHealthSamples"
        const val DELETIONS_FOLDER = "healthDeletions"
        const val STAGED_LIMIT = 100
        const val RECORD_AGE_SECONDS = 60L
        const val DIASTOLIC = 76.0
        const val MAX_ARCHIVE_BYTES = 1L * 1024 * 1024
    }
}
