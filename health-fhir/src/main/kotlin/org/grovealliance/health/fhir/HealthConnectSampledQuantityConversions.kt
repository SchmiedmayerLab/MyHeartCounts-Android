//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import java.time.Instant

internal fun HealthConnectConverter.convertCyclingCadence(
    record: CyclingPedalingCadenceRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertSampleSeries(
    metadata = record.metadata,
    recordType = HealthConnectConverter.CYCLING_PEDALING_CADENCE_RECORD,
    start = record.startTime,
    end = record.endTime,
    samples = record.samples.map { SeriesSample(it.time, it.revolutionsPerMinute) },
    spec = MobileQuantitySpec(
        profile = HealthConnectContract.MOBILE_CYCLING_CADENCE_PROFILE,
        category = HealthConnectConverter.ACTIVITY_CATEGORY,
        codeSystem = HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
        code = "cycling-cadence",
        display = "Cycling cadence",
        unitCode = "/min",
        unitDisplay = "revolutions/minute",
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertPower(
    record: PowerRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertSampleSeries(
    metadata = record.metadata,
    recordType = HealthConnectConverter.POWER_RECORD,
    start = record.startTime,
    end = record.endTime,
    samples = record.samples.map { SeriesSample(it.time, it.power.inWatts) },
    spec = MobileQuantitySpec(
        profile = HealthConnectContract.MOBILE_POWER_PROFILE,
        category = HealthConnectConverter.ACTIVITY_CATEGORY,
        codeSystem = HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
        code = "power",
        display = "Power",
        unitCode = "W",
        unitDisplay = "W",
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertSpeed(
    record: SpeedRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertSampleSeries(
    metadata = record.metadata,
    recordType = HealthConnectConverter.SPEED_RECORD,
    start = record.startTime,
    end = record.endTime,
    samples = record.samples.map { SeriesSample(it.time, it.speed.inMetersPerSecond) },
    spec = MobileQuantitySpec(
        profile = HealthConnectContract.MOBILE_SPEED_PROFILE,
        category = HealthConnectConverter.ACTIVITY_CATEGORY,
        codeSystem = HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
        code = "speed",
        display = "Speed",
        unitCode = "m/s",
        unitDisplay = "m/s",
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertStepCadence(
    record: StepsCadenceRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertSampleSeries(
    metadata = record.metadata,
    recordType = HealthConnectConverter.STEPS_CADENCE_RECORD,
    start = record.startTime,
    end = record.endTime,
    samples = record.samples.map { SeriesSample(it.time, it.rate) },
    spec = MobileQuantitySpec(
        profile = HealthConnectContract.HEALTH_CONNECT_STEP_CADENCE_PROFILE,
        category = HealthConnectConverter.ACTIVITY_CATEGORY,
        codeSystem = HealthConnectContract.HEALTH_CONNECT_MEASUREMENT,
        code = "step-cadence",
        display = "Step cadence",
        unitCode = "{steps}/min",
        unitDisplay = "steps/minute",
        adapterSpecific = true,
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertSkinTemperature(
    record: SkinTemperatureRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion {
    val baseline = record.baseline
    if (record.deltas.isNotEmpty() && baseline == null) {
        throw InvalidHealthConnectRecord(
            "SkinTemperatureRecord deltas require an explicit baseline to state absolute skin temperatures.",
        )
    }
    return convertSampleSeries(
        metadata = record.metadata,
        recordType = HealthConnectConverter.SKIN_TEMPERATURE_RECORD,
        start = record.startTime,
        end = record.endTime,
        samples = record.deltas.map { delta ->
            SeriesSample(delta.time, requireNotNull(baseline).inCelsius + delta.delta.inCelsius)
        },
        spec = MobileQuantitySpec(
            profile = HealthConnectContract.MOBILE_SKIN_TEMPERATURE_PROFILE,
            category = HealthConnectConverter.VITAL_SIGNS_CATEGORY,
            codeSystem = HealthConnectContract.LOINC,
            code = "61008-9",
            display = "Body surface temperature",
            unitCode = "Cel",
            unitDisplay = "Cel",
            valueDomain = QuantityValueDomain.UNBOUNDED,
        ),
        bodySite = skinTemperatureMeasurementLocation(record.measurementLocation),
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )
}
