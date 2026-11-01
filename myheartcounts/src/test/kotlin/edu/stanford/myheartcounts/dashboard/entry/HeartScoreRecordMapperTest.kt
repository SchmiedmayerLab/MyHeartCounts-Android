//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.entry

import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import org.grovealliance.core.time.FakeTimeProvider
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class HeartScoreRecordMapperTest {
    private val time = FakeTimeProvider().apply { setZone(ZoneOffset.ofHours(2)) }
    private val mapper = HeartScoreRecordMapper(timeProvider = time)
    private val date = Instant.parse("2025-10-26T12:00:00Z")

    @Test
    fun `glucose saves fasting metadata and preserves the date units and retry identity`() {
        // given
        val entry = HeartScoreEntry.Quantity(
            id = "glucose",
            date = date,
            component = HeartScoreComponent.BloodGlucose,
            value = 90.0,
        )

        // when
        val record = mapper.records(entry = entry).single() as BloodGlucoseRecord

        // then
        assertThat(record.level.inMilligramsPerDeciliter).isEqualTo(90.0)
        assertThat(record.relationToMeal).isEqualTo(BloodGlucoseRecord.RELATION_TO_MEAL_FASTING)
        assertThat(record.time).isEqualTo(date)
        assertThat(record.zoneOffset).isEqualTo(ZoneOffset.ofHours(2))
        assertThat(record.metadata.clientRecordId).isEqualTo("glucose")
        assertThat(record.metadata.recordingMethod).isEqualTo(Metadata.RECORDING_METHOD_MANUAL_ENTRY)
    }

    @Test
    fun `pressure saves one paired native record`() {
        // when
        val record = mapper.records(entry = HeartScoreEntry.Pressure(
            id = "bp",
            date = date,
            systolic = 120.0,
            diastolic = 80.0,
        ))
            .single() as BloodPressureRecord

        // then
        assertThat(record.systolic.inMillimetersOfMercury).isEqualTo(120.0)
        assertThat(record.diastolic.inMillimetersOfMercury).isEqualTo(80.0)
    }

    @Test
    fun `calculated BMI saves both physical measurements at the same instant`() {
        // when
        val records = mapper.records(entry = HeartScoreEntry.BodyMassIndex(
            id = "bmi",
            date = date,
            value = 25.0,
            weightKg = 81.0,
            heightMeters = 1.8,
        ))

        // then
        assertThat((records[0] as HeightRecord).height.inMeters).isEqualTo(1.8)
        assertThat((records[1] as WeightRecord).weight.inKilograms).isEqualTo(81.0)
        assertThat(records.map { it.metadata.clientRecordId }).containsExactly("bmi-height", "bmi-weight")
    }

    @Test
    fun `LDL and direct BMI never create synthetic native records`() {
        // then
        assertThat(mapper.records(entry = HeartScoreEntry.Quantity(
            id = "ldl",
            date = date,
            component = HeartScoreComponent.BloodLipids,
            value = 110.0,
        ))).isEmpty()
        assertThat(mapper.records(entry = HeartScoreEntry.BodyMassIndex(
            id = "bmi",
            date = date,
            value = 25.0,
            weightKg = null,
            heightMeters = null,
        ))).isEmpty()
    }
}
