//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.Temperature
import androidx.health.connect.client.units.TemperatureDelta
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class HealthConnectR4ConverterSeriesTest : HealthConnectR4ConverterTestSupport() {
    @Test
    fun `maps resting heart rate onto its stated instant without inventing aggregation`() {
        val observation = converter.convert(
            RestingHeartRateRecord(
                time = Instant.parse("2026-08-19T15:15:00Z"),
                zoneOffset = ZoneOffset.ofHours(-7),
                beatsPerMinute = 61,
                metadata = metadata(Metadata.autoRecorded(device), id = "resting-heart-rate"),
            ),
            convertedAt,
        ).observations.single()

        assertThat(observation.meta.profile.map { it.value }).containsExactly(
            HealthConnectContract.MOBILE_RESTING_HEART_RATE_PROFILE,
            HealthConnectContract.HEALTH_CONNECT_OBSERVATION_PROFILE,
        ).inOrder()
        assertThat(observation.code.coding.map { it.system to it.code }).containsExactly(
            HealthConnectContract.LOINC to "40443-4",
            HealthConnectContract.LOINC to "8867-4",
        ).inOrder()
        assertThat(observation.category.single().coding.single().system)
            .isEqualTo(HealthConnectContract.OBSERVATION_CATEGORY)
        assertThat(observation.category.single().coding.single().code).isEqualTo("vital-signs")
        assertThat(observation.effectiveDateTimeType.valueAsString)
            .isEqualTo("2026-08-19T08:15:00-07:00")
        assertThat(observation.hasMethod()).isFalse()
        assertThat(observation.valueQuantity.value.toLong()).isEqualTo(61)
        assertThat(observation.valueQuantity.code).isEqualTo("/min")
    }

    @Test
    fun `maps each power sample and keeps same-instant occurrence identities distinct`() {
        val start = Instant.parse("2026-08-19T16:00:00Z")
        val result = converter.convert(
            PowerRecord(
                startTime = start,
                startZoneOffset = ZoneOffset.ofHours(-7),
                endTime = start.plusSeconds(60),
                endZoneOffset = ZoneOffset.ofHours(-7),
                samples = listOf(
                    PowerRecord.Sample(start.plusSeconds(15), Power.watts(215.5)),
                    PowerRecord.Sample(start.plusSeconds(15), Power.watts(220.0)),
                    PowerRecord.Sample(start.plusSeconds(45), Power.watts(215.5)),
                ),
                metadata = metadata(Metadata.autoRecorded(device), id = "power-series"),
            ),
            convertedAt,
        )

        assertThat(result.observations).hasSize(3)
        assertThat(result.observationIdentifiers.map { it.value }.distinct()).hasSize(3)
        result.observations.forEach { observation ->
            assertThat(observation.meta.profile.map { it.value })
                .contains(HealthConnectContract.MOBILE_POWER_PROFILE)
            assertThat(observation.code.codingFirstRep.code).isEqualTo("power")
            assertThat(observation.valueQuantity.code).isEqualTo("W")
            assertThat(observation.hasEffectiveDateTimeType()).isTrue()
        }
        assertThat(
            result.observations.map { it.effectiveDateTimeType.value.time to it.valueQuantity.value.toPlainString() },
        ).containsExactly(
            start.plusSeconds(15).toEpochMilli() to "215.5",
            start.plusSeconds(15).toEpochMilli() to "220.0",
            start.plusSeconds(45).toEpochMilli() to "215.5",
        )
    }

    @Test
    fun `power sample identity is stable across replay and sample order`() {
        val start = Instant.parse("2026-08-19T16:00:00Z")
        val samples = listOf(
            PowerRecord.Sample(start.plusSeconds(15), Power.watts(215.5)),
            PowerRecord.Sample(start.plusSeconds(45), Power.watts(220.0)),
        )
        val record = { ordered: List<PowerRecord.Sample> ->
            PowerRecord(
                startTime = start,
                startZoneOffset = ZoneOffset.UTC,
                endTime = start.plusSeconds(60),
                endZoneOffset = ZoneOffset.UTC,
                samples = ordered,
                metadata = metadata(Metadata.autoRecorded(device), id = "power-replay"),
            )
        }

        val first = converter.convert(record(samples), convertedAt).observationIdentifiers.map { it.value }
        val replayed = converter.convert(record(samples.reversed()), convertedAt)
            .observationIdentifiers
            .map { it.value }

        assertThat(replayed).containsExactlyElementsIn(first).inOrder()
    }

    @Test
    fun `maps an empty steps-cadence series to a local zero-output conversion`() {
        val instant = Instant.parse("2026-08-19T16:00:00Z")
        val result = converter.convert(
            StepsCadenceRecord(
                startTime = instant,
                startZoneOffset = ZoneOffset.UTC,
                endTime = instant,
                endZoneOffset = ZoneOffset.UTC,
                samples = emptyList(),
                metadata = metadata(Metadata.autoRecorded(device), id = "empty-cadence"),
            ),
            convertedAt,
        )

        assertThat(result.observations).isEmpty()
        assertThat(result.provenance).isNull()
    }

    @Test
    fun `converts skin-temperature deltas against the explicit baseline`() {
        val start = Instant.parse("2026-08-19T02:00:00Z")
        val result = converter.convert(
            SkinTemperatureRecord(
                startTime = start,
                startZoneOffset = ZoneOffset.UTC,
                endTime = start.plusSeconds(7_200),
                endZoneOffset = ZoneOffset.UTC,
                metadata = metadata(Metadata.autoRecorded(device), id = "skin-temperature"),
                deltas = listOf(
                    SkinTemperatureRecord.Delta(start.plusSeconds(600), TemperatureDelta.celsius(-0.25)),
                    SkinTemperatureRecord.Delta(start.plusSeconds(1_200), TemperatureDelta.celsius(0.5)),
                ),
                baseline = Temperature.celsius(33.5),
                measurementLocation = SkinTemperatureRecord.MEASUREMENT_LOCATION_WRIST,
            ),
            convertedAt,
        )

        assertThat(result.observations).hasSize(2)
        assertThat(result.observationIdentifiers.map { it.value }.distinct()).hasSize(2)
        result.observations.forEach { observation ->
            assertThat(observation.meta.profile.map { it.value })
                .contains(HealthConnectContract.MOBILE_SKIN_TEMPERATURE_PROFILE)
            assertThat(observation.code.codingFirstRep.code).isEqualTo("61008-9")
            assertThat(observation.valueQuantity.code).isEqualTo("Cel")
            assertThat(observation.bodySite.codingFirstRep.code).isEqualTo("8205005")
        }
        assertThat(result.observations.map { it.valueQuantity.value.toDouble() })
            .containsExactly(33.25, 34.0)
            .inOrder()
    }

    @Test
    fun `fails closed for skin-temperature deltas without a baseline`() {
        val start = Instant.parse("2026-08-19T02:00:00Z")
        val record = SkinTemperatureRecord(
            startTime = start,
            startZoneOffset = ZoneOffset.UTC,
            endTime = start.plusSeconds(7_200),
            endZoneOffset = ZoneOffset.UTC,
            metadata = metadata(Metadata.autoRecorded(device), id = "skin-temperature-no-baseline"),
            deltas = listOf(
                SkinTemperatureRecord.Delta(start.plusSeconds(600), TemperatureDelta.celsius(-0.25)),
            ),
        )

        assertThrows(InvalidHealthConnectRecord::class.java) {
            converter.convert(record, convertedAt)
        }
    }
}
