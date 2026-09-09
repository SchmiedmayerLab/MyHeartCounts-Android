//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.firebase

import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Velocity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.luben.zstd.Zstd
import com.google.common.truth.Truth.assertThat
import com.google.firebase.storage.FirebaseStorage
import edu.stanford.myheartcounts.standard.health.HealthStagingDatabase
import edu.stanford.myheartcounts.standard.health.HealthUploadSchedule
import edu.stanford.myheartcounts.standard.health.MHCHealthDataHandler
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.grovealliance.core.requireDependency
import org.grovealliance.health.RecordType
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneOffset

/**
 * Exercises the health pipeline against Cloud Storage, driven by synthetic records rather than by
 * Health Connect.
 *
 * Health Connect is deliberately out of the loop: an emulator has no health data, and the app only
 * asks for read access so it cannot seed any. Handing records straight to the handler covers
 * everything downstream of collection — FHIR mapping, the retention window, zstd, and the upload —
 * which is all of the code this repo owns.
 */
@RunWith(AndroidJUnit4::class)
class HealthUploadTest {

    private val handler: MHCHealthDataHandler get() = requireDependency()
    private val staging: HealthStagingDatabase get() = requireDependency()

    @Before
    fun signIn() = runTest {
        assumeTrue("Needs -Pmhc.firebaseEmulatorHost", FirebaseTestSession.isEmulatorConfigured)
        FirebaseTestSession.initializeFirebase()
        FirebaseTestSession.signUpFreshParticipant()
        handler.clearPendingUploads()
    }

    @After
    fun signOut() = runTest {
        handler.clearPendingUploads()
        FirebaseTestSession.signOut()
    }

    @Test
    fun stagesSharedSampleTypesAndUploadsOnlyThosePastTheRetentionWindow() = runTest {
        val old = stepsRecord(end = Instant.now().minusSeconds(RETENTION_SECONDS + BUFFER_SECONDS))
        val fresh = stepsRecord(end = Instant.now())
        handler.handleNewRecords(records = setOf(old, fresh), type = RecordType.steps)

        handler.uploadStagedData().getOrThrow()

        val archives = archivesIn(folder = "liveHealthSamples")
        assertThat(archives).hasSize(1)
        // The backend recovers the sample type by splitting the filename on the underscore, and it
        // expects the full HealthKit identifier.
        assertThat(archives.single().name).startsWith("HKQuantityTypeIdentifierStepCount_")
        assertThat(archives.single().name).endsWith(".json.zstd")

        // The fresh sample must still be held back rather than swept along with the old one.
        val remaining = staging.dao().observationsReadyForUpload(
            sampleTypeIdentifier = "HKQuantityTypeIdentifierStepCount",
            cutoffMillis = Long.MAX_VALUE,
            limit = 10,
        )
        assertThat(remaining).hasSize(1)
    }

    @Test
    fun uploadsADecompressibleArchiveOfFhirObservations() = runTest {
        handler.handleNewRecords(
            records = setOf(stepsRecord(end = Instant.now().minusSeconds(RETENTION_SECONDS + BUFFER_SECONDS))),
            type = RecordType.steps,
        )
        handler.uploadStagedData().getOrThrow()

        // Each test signs up its own participant, so this prefix holds only what this test uploaded.
        val archive = archivesIn(folder = "liveHealthSamples").single()
        val json = String(Zstd.decompress(archive.getBytes(MAX_ARCHIVE_BYTES).await(), MAX_DECOMPRESSED))

        assertThat(json).startsWith("[")
        assertThat(json).contains("\"resourceType\":\"Observation\"")
        assertThat(json).contains("\"41950-7\"")
        // Provenance the study needs to separate Health Connect data from HealthKit data.
        assertThat(json).contains("source-platform")
    }

    @Test
    fun sendsAnAndroidOnlySampleTypeStraightToStorage() = runTest {
        // Speed has no HealthKit counterpart worth sharing a collection with, so it skips staging
        // and goes out immediately under an Android-native identifier.
        handler.handleNewRecords(records = setOf(speedRecord()), type = RecordType.speed)

        val archives = archivesIn(folder = "liveHealthSamples")
        assertThat(archives).hasSize(1)
        assertThat(archives.single().name).startsWith("HealthConnectTypeIdentifierSpeed_")
    }

    private suspend fun archivesIn(folder: String) = FirebaseStorage.getInstance().reference
        .child("users/${FirebaseTestSession.firestore.accountId}/$folder")
        .listAll()
        .await()
        .items

    private fun stepsRecord(end: Instant) = StepsRecord(
        startTime = end.minusSeconds(STEP_INTERVAL_SECONDS),
        startZoneOffset = ZoneOffset.UTC,
        endTime = end,
        endZoneOffset = ZoneOffset.UTC,
        count = 1_234,
        metadata = Metadata.manualEntry(),
    )

    private fun speedRecord() = SpeedRecord(
        startTime = Instant.now().minusSeconds(STEP_INTERVAL_SECONDS),
        startZoneOffset = ZoneOffset.UTC,
        endTime = Instant.now(),
        endZoneOffset = ZoneOffset.UTC,
        samples = listOf(
            SpeedRecord.Sample(
                time = Instant.now().minusSeconds(1),
                speed = Velocity.metersPerSecond(1.4),
            ),
        ),
        metadata = Metadata.manualEntry(),
    )

    private companion object {
        val RETENTION_SECONDS = HealthUploadSchedule.RETENTION_OFFSET_DAYS * 24 * 60 * 60
        const val BUFFER_SECONDS = 3_600L
        const val STEP_INTERVAL_SECONDS = 600L
        const val MAX_ARCHIVE_BYTES = 1L * 1024 * 1024
        const val MAX_DECOMPRESSED = 8 * 1024 * 1024
    }
}
