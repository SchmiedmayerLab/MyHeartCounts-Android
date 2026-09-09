//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health

import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Pressure
import com.google.common.truth.Truth.assertThat
import org.grovealliance.health.RecordType
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Quantity
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class HealthObservationMapperTest {

    private val mapper = HealthObservationMapper()
    private val issuedAt: Instant = Instant.parse("2026-01-05T12:00:00Z")
    private val start: Instant = Instant.parse("2026-01-05T08:00:00Z")
    private val end: Instant = Instant.parse("2026-01-05T09:00:00Z")

    @Test
    fun `it should map a steps record onto the LOINC code the backend defines`() {
        val record = StepsRecord(
            startTime = start,
            startZoneOffset = ZoneOffset.UTC,
            endTime = end,
            endZoneOffset = ZoneOffset.UTC,
            count = 4_200,
            metadata = Metadata.manualEntry(),
        )

        val observation = mapper.map(
            record = record,
            sampleType = HealthSampleType.of(recordType = RecordType.steps),
            issuedAt = issuedAt,
        ).single()

        assertThat(observation.status).isEqualTo(Observation.ObservationStatus.FINAL)
        assertThat(observation.code.codingFirstRep.system).isEqualTo("http://loinc.org")
        assertThat(observation.code.codingFirstRep.code).isEqualTo("41950-7")

        val quantity = observation.value as Quantity
        assertThat(quantity.value.toDouble()).isEqualTo(4_200.0)
        assertThat(quantity.code).isEqualTo("steps")
        assertThat(quantity.system).isEqualTo("http://unitsofmeasure.org")

        assertThat(observation.issued.toInstant()).isEqualTo(issuedAt)
    }

    @Test
    fun `it should record an interval as a period and a point measurement as a dateTime`() {
        val steps = StepsRecord(
            startTime = start,
            startZoneOffset = ZoneOffset.UTC,
            endTime = end,
            endZoneOffset = ZoneOffset.UTC,
            count = 10,
            metadata = Metadata.manualEntry(),
        )
        val interval = mapper.map(
            record = steps,
            sampleType = HealthSampleType.of(recordType = RecordType.steps),
            issuedAt = issuedAt,
        ).single()

        val period = interval.effective as Period
        assertThat(period.start.toInstant()).isEqualTo(start)
        assertThat(period.end.toInstant()).isEqualTo(end)

        val bloodPressure = bloodPressureRecord()
        val instantaneous = mapper.map(
            record = bloodPressure,
            sampleType = HealthSampleType.of(recordType = RecordType.bloodPressure),
            issuedAt = issuedAt,
        ).single()

        assertThat((instantaneous.effective as DateTimeType).value.toInstant()).isEqualTo(start)
    }

    @Test
    fun `it should carry both blood pressure readings as components`() {
        val observation = mapper.map(
            record = bloodPressureRecord(),
            sampleType = HealthSampleType.of(recordType = RecordType.bloodPressure),
            issuedAt = issuedAt,
        ).single()

        assertThat(observation.code.codingFirstRep.code).isEqualTo("85354-9")
        assertThat(observation.hasValue()).isFalse()

        val byCode = observation.component.associateBy { it.code.codingFirstRep.code }
        assertThat(byCode.keys).containsExactly("8480-6", "8462-4")
        assertThat((byCode.getValue("8480-6").value as Quantity).value.toDouble()).isEqualTo(120.0)
        assertThat((byCode.getValue("8462-4").value as Quantity).value.toDouble()).isEqualTo(80.0)
        assertThat((byCode.getValue("8480-6").value as Quantity).code).isEqualTo("mm[Hg]")
    }

    @Test
    fun `it should produce one observation per heart rate reading`() {
        // HealthKit delivers each beat measurement as its own sample, so a Health Connect record
        // holding a series has to be split for the two platforms' data to line up.
        val record = HeartRateRecord(
            startTime = start,
            startZoneOffset = ZoneOffset.UTC,
            endTime = end,
            endZoneOffset = ZoneOffset.UTC,
            samples = listOf(
                HeartRateRecord.Sample(time = start, beatsPerMinute = 62),
                HeartRateRecord.Sample(time = start.plusSeconds(60), beatsPerMinute = 71),
            ),
            metadata = Metadata.manualEntry(),
        )

        val observations = mapper.map(
            record = record,
            sampleType = HealthSampleType.of(recordType = RecordType.heartRate),
            issuedAt = issuedAt,
        )

        assertThat(observations).hasSize(2)
        assertThat(observations.map { (it.value as Quantity).value.toDouble() })
            .containsExactly(62.0, 71.0)
        // Distinct ids, so a re-upload replaces each reading rather than collapsing the series.
        assertThat(observations.map { it.id }.toSet()).hasSize(2)
    }

    @Test
    fun `it should stamp the provenance every uploaded observation needs`() {
        val observation = mapper.map(
            record = bloodPressureRecord(),
            sampleType = HealthSampleType.of(recordType = RecordType.bloodPressure),
            issuedAt = issuedAt,
        ).single()

        HealthObservationProvenance(
            appRevision = "1.2.3",
            studyId = "study-id",
            studyRevision = 4,
        ).applyTo(observation = observation)

        val byUrl = observation.extension.associateBy { it.url }
        assertThat(byUrl.keys).containsExactly(
            HealthObservationProvenance.SAMPLE_UPLOAD_TIME_ZONE_URL,
            HealthObservationProvenance.APP_REVISION_URL,
            HealthObservationProvenance.PLATFORM_URL,
            HealthObservationProvenance.STUDY_ENROLLMENT_URL,
        )
        assertThat(byUrl.getValue(HealthObservationProvenance.APP_REVISION_URL).value.primitiveValue())
            .isEqualTo("1.2.3")
        assertThat(byUrl.getValue(HealthObservationProvenance.PLATFORM_URL).value.primitiveValue())
            .isEqualTo(HealthObservationProvenance.ANDROID_PLATFORM)

        val enrollment = byUrl.getValue(HealthObservationProvenance.STUDY_ENROLLMENT_URL)
        assertThat(enrollment.extension.map { it.url }).containsExactly(
            "${HealthObservationProvenance.STUDY_ENROLLMENT_URL}/study-id",
            "${HealthObservationProvenance.STUDY_ENROLLMENT_URL}/study-revision",
        )
    }

    @Test
    fun `it should drop the enrollment extension for an unenrolled participant`() {
        val observation = mapper.map(
            record = bloodPressureRecord(),
            sampleType = HealthSampleType.of(recordType = RecordType.bloodPressure),
            issuedAt = issuedAt,
        ).single()

        HealthObservationProvenance(appRevision = "1.0.0", studyId = "id", studyRevision = 1)
            .applyTo(observation = observation)
        HealthObservationProvenance(appRevision = "1.0.0", studyId = null, studyRevision = null)
            .applyTo(observation = observation)

        assertThat(observation.extension.map { it.url })
            .doesNotContain(HealthObservationProvenance.STUDY_ENROLLMENT_URL)
    }

    private fun bloodPressureRecord() = BloodPressureRecord(
        time = start,
        zoneOffset = ZoneOffset.UTC,
        systolic = Pressure.millimetersOfMercury(120.0),
        diastolic = Pressure.millimetersOfMercury(80.0),
        metadata = Metadata.manualEntry(),
    )
}
