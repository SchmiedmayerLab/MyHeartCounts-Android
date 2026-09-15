//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health

import com.google.common.truth.Truth.assertThat
import org.grovealliance.health.RecordType
import org.junit.Test

class HealthSampleTypeTest {

    @Test
    fun `it should name a shared record type with its full HealthKit identifier`() {
        // The backend keys collections and archive filenames on the full identifier — the collection
        // is HealthObservations_HKQuantityTypeIdentifierStepCount, not ..._stepCount — so a short
        // name here would silently start a second, parallel dataset.
        val sampleType = HealthSampleType.of(recordType = RecordType.steps)

        assertThat(sampleType.identifier).isEqualTo("HKQuantityTypeIdentifierStepCount")
        assertThat(sampleType.strategy).isEqualTo(HealthUploadStrategy.QUEUE_LOCALLY)
    }

    @Test
    fun `it should keep heart rate variability out of the HealthKit SDNN collection`() {
        // Health Connect reports RMSSD and HealthKit reports SDNN. Both summarize the same beat
        // intervals but are not interchangeable, so they must never share a collection.
        val sampleType = HealthSampleType.of(recordType = RecordType.heartRateVariabilityRmssd)

        assertThat(sampleType.identifier).doesNotContain("HKQuantityTypeIdentifier")
        assertThat(sampleType.identifier).isEqualTo("HealthConnectTypeIdentifierHeartRateVariabilityRmssd")
        assertThat(HealthSampleType.isDeliberatelyAndroidOnly(RecordType.heartRateVariabilityRmssd)).isTrue()
    }

    @Test
    fun `it should send a record type only Android produces straight to storage`() {
        val sampleType = HealthSampleType.of(recordType = RecordType.speed)

        assertThat(sampleType.strategy).isEqualTo(HealthUploadStrategy.FIREBASE_STORAGE)
    }

    @Test
    fun `it should map blood pressure onto the HealthKit correlation type`() {
        val sampleType = HealthSampleType.of(recordType = RecordType.bloodPressure)

        assertThat(sampleType.identifier).isEqualTo("HKCorrelationTypeIdentifierBloodPressure")
    }

    @Test
    fun `it should give every shared sample type a coding the backend defines`() {
        // An observation carrying a code the backend's converters do not know is dropped on
        // ingestion, so a missing entry here is data loss rather than a cosmetic gap.
        val sharedIdentifiers = listOf(
            RecordType.activeCaloriesBurned,
            RecordType.bloodGlucose,
            RecordType.bloodPressure,
            RecordType.bodyFat,
            RecordType.bodyTemperature,
            RecordType.distance,
            RecordType.floorsClimbed,
            RecordType.heartRate,
            RecordType.height,
            RecordType.leanBodyMass,
            RecordType.oxygenSaturation,
            RecordType.respiratoryRate,
            RecordType.restingHeartRate,
            RecordType.steps,
            RecordType.vo2Max,
            RecordType.weight,
        ).map { HealthSampleType.of(recordType = it).identifier }

        sharedIdentifiers.forEach { identifier ->
            assertThat(HealthObservationCoding.forSampleType(sampleTypeIdentifier = identifier))
                .isNotNull()
        }
    }

    @Test
    fun `it should express body height in the centimetres the backend expects`() {
        val coding = HealthObservationCoding.forSampleType(
            sampleTypeIdentifier = "HKQuantityTypeIdentifierHeight",
        )

        assertThat(coding?.unit).isEqualTo("cm")
    }
}
