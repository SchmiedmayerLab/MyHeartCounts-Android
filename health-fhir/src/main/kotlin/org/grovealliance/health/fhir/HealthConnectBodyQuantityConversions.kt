//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import org.grovealliance.health.RecordType
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import java.time.Instant

internal fun HealthConnectConverter.convertActiveEnergy(
    record: ActiveCaloriesBurnedRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertIntervalQuantity(
    metadata = record.metadata,
    recordType = RecordType.activeCaloriesBurned.identifier,
    start = record.startTime,
    startOffset = record.startZoneOffset,
    end = record.endTime,
    endOffset = record.endZoneOffset,
    value = record.energy.inKilocalories,
    spec = MobileQuantitySpec(
        profile = HealthConnectContract.MOBILE_ACTIVE_ENERGY_PROFILE,
        category = HealthConnectConverter.ACTIVITY_CATEGORY,
        codeSystem = HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
        code = "active-energy-burned",
        display = "Active energy burned",
        unitCode = "kcal",
        unitDisplay = "kcal",
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertBasalMetabolicRate(
    record: BasalMetabolicRateRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertInstantQuantity(
    metadata = record.metadata,
    recordType = RecordType.basalMetabolicRate.identifier,
    time = record.time,
    offset = record.zoneOffset,
    value = record.basalMetabolicRate.inKilocaloriesPerDay,
    spec = MobileQuantitySpec(
        profile = HealthConnectContract.HEALTH_CONNECT_BASAL_METABOLIC_RATE_PROFILE,
        category = HealthConnectConverter.ACTIVITY_CATEGORY,
        codeSystem = HealthConnectContract.HEALTH_CONNECT_MEASUREMENT,
        code = "basal-metabolic-rate",
        display = "Basal metabolic rate",
        unitCode = "kcal/d",
        unitDisplay = "kcal/day",
        adapterSpecific = true,
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertBodyFat(
    record: BodyFatRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertInstantQuantity(
    metadata = record.metadata,
    recordType = RecordType.bodyFat.identifier,
    time = record.time,
    offset = record.zoneOffset,
    value = record.percentage.value,
    spec = MobileQuantitySpec(
        profile = HealthConnectContract.MOBILE_BODY_FAT_PERCENTAGE_PROFILE,
        category = HealthConnectConverter.VITAL_SIGNS_CATEGORY,
        codeSystem = HealthConnectContract.LOINC,
        code = "41982-0",
        display = "Percentage of body fat Measured",
        unitCode = "%",
        unitDisplay = "%",
        valueDomain = HealthConnectContract.quantityValueDomains.getValue("body-fat-percentage"),
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertBodyWaterMass(
    record: BodyWaterMassRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertInstantQuantity(
    metadata = record.metadata,
    recordType = RecordType.bodyWaterMass.identifier,
    time = record.time,
    offset = record.zoneOffset,
    value = record.mass.inKilograms,
    spec = MobileQuantitySpec(
        profile = HealthConnectContract.MOBILE_BODY_WATER_MASS_PROFILE,
        category = HealthConnectConverter.VITAL_SIGNS_CATEGORY,
        codeSystem = HealthConnectContract.LOINC,
        code = "101683-1",
        display = "Body water mass",
        unitCode = "kg",
        unitDisplay = "kg",
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertBoneMass(
    record: BoneMassRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertInstantQuantity(
    metadata = record.metadata,
    recordType = RecordType.boneMass.identifier,
    time = record.time,
    offset = record.zoneOffset,
    value = record.mass.inKilograms,
    spec = MobileQuantitySpec(
        profile = HealthConnectContract.MOBILE_BONE_MASS_PROFILE,
        category = HealthConnectConverter.VITAL_SIGNS_CATEGORY,
        codeSystem = HealthConnectContract.LOINC,
        code = "101685-6",
        display = "Body bone mass",
        unitCode = "kg",
        unitDisplay = "kg",
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertLeanBodyMass(
    record: LeanBodyMassRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertInstantQuantity(
    metadata = record.metadata,
    recordType = RecordType.leanBodyMass.identifier,
    time = record.time,
    offset = record.zoneOffset,
    value = record.mass.inKilograms,
    spec = MobileQuantitySpec(
        profile = HealthConnectContract.MOBILE_LEAN_BODY_MASS_PROFILE,
        category = HealthConnectConverter.VITAL_SIGNS_CATEGORY,
        codeSystem = HealthConnectContract.LOINC,
        code = "91557-9",
        display = "Lean body weight",
        unitCode = "kg",
        unitDisplay = "kg",
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertHeartRateVariabilityRmssd(
    record: HeartRateVariabilityRmssdRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertInstantQuantity(
    metadata = record.metadata,
    recordType = RecordType.heartRateVariabilityRmssd.identifier,
    time = record.time,
    offset = record.zoneOffset,
    value = record.heartRateVariabilityMillis,
    spec = MobileQuantitySpec(
        profile = HealthConnectContract.MOBILE_HEART_RATE_VARIABILITY_RMSSD_PROFILE,
        category = HealthConnectConverter.VITAL_SIGNS_CATEGORY,
        codeSystem = HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
        code = "heart-rate-variability-rmssd",
        display = "Heart rate variability RMSSD",
        unitCode = "ms",
        unitDisplay = "ms",
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertVo2Max(
    record: Vo2MaxRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertInstantQuantity(
    metadata = record.metadata,
    recordType = RecordType.vo2Max.identifier,
    time = record.time,
    offset = record.zoneOffset,
    value = record.vo2MillilitersPerMinuteKilogram,
    spec = MobileQuantitySpec(
        profile = HealthConnectContract.MOBILE_VO2_MAX_PROFILE,
        category = HealthConnectConverter.ACTIVITY_CATEGORY,
        codeSystem = HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
        code = "vo2-max",
        display = "VO2 max",
        unitCode = "mL/kg/min",
        unitDisplay = "mL/kg/min",
    ),
    method = CodeableConcept(vo2MaxMeasurementMethodCoding(record.measurementMethod)),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertHydration(
    record: HydrationRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertIntervalQuantity(
    metadata = record.metadata,
    recordType = RecordType.hydration.identifier,
    start = record.startTime,
    startOffset = record.startZoneOffset,
    end = record.endTime,
    endOffset = record.endZoneOffset,
    value = record.volume.inMilliliters,
    spec = MobileQuantitySpec(
        profile = HealthConnectContract.MOBILE_FLUID_INTAKE_PROFILE,
        category = null,
        codeSystem = HealthConnectContract.LOINC,
        code = "8985-4",
        display = "Fluid intake Measured",
        unitCode = "mL",
        unitDisplay = "mL",
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertElevationGained(
    record: ElevationGainedRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertIntervalQuantity(
    metadata = record.metadata,
    recordType = RecordType.elevationGained.identifier,
    start = record.startTime,
    startOffset = record.startZoneOffset,
    end = record.endTime,
    endOffset = record.endZoneOffset,
    value = record.elevation.inMeters,
    spec = MobileQuantitySpec(
        profile = HealthConnectContract.HEALTH_CONNECT_ELEVATION_GAINED_PROFILE,
        category = HealthConnectConverter.ACTIVITY_CATEGORY,
        codeSystem = HealthConnectContract.HEALTH_CONNECT_MEASUREMENT,
        code = "elevation-gained",
        display = "Elevation gained",
        unitCode = "m",
        unitDisplay = "m",
        // Health Connect admits negative elevation change over an interval.
        valueDomain = QuantityValueDomain.UNBOUNDED,
        adapterSpecific = true,
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertFloorsClimbed(
    record: FloorsClimbedRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertIntervalQuantity(
    metadata = record.metadata,
    recordType = RecordType.floorsClimbed.identifier,
    start = record.startTime,
    startOffset = record.startZoneOffset,
    end = record.endTime,
    endOffset = record.endZoneOffset,
    value = record.floors,
    spec = MobileQuantitySpec(
        profile = HealthConnectContract.MOBILE_FLIGHTS_CLIMBED_PROFILE,
        category = HealthConnectConverter.ACTIVITY_CATEGORY,
        codeSystem = HealthConnectContract.LOINC,
        code = "100304-5",
        display = "Flights climbed [#] Reporting Period",
        unitCode = "{flights}",
        unitDisplay = "flights",
        valueDomain = HealthConnectContract.quantityValueDomains.getValue("flights-climbed"),
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertTotalEnergy(
    record: TotalCaloriesBurnedRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertIntervalQuantity(
    metadata = record.metadata,
    recordType = RecordType.totalCaloriesBurned.identifier,
    start = record.startTime,
    startOffset = record.startZoneOffset,
    end = record.endTime,
    endOffset = record.endZoneOffset,
    value = record.energy.inKilocalories,
    spec = MobileQuantitySpec(
        profile = HealthConnectContract.HEALTH_CONNECT_TOTAL_ENERGY_PROFILE,
        category = HealthConnectConverter.ACTIVITY_CATEGORY,
        codeSystem = HealthConnectContract.HEALTH_CONNECT_MEASUREMENT,
        code = "total-energy-burned",
        display = "Total energy burned",
        unitCode = "kcal",
        unitDisplay = "kcal",
        adapterSpecific = true,
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertWheelchairPushes(
    record: WheelchairPushesRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertIntervalQuantity(
    metadata = record.metadata,
    recordType = RecordType.wheelchairPushes.identifier,
    start = record.startTime,
    startOffset = record.startZoneOffset,
    end = record.endTime,
    endOffset = record.endZoneOffset,
    value = record.count.toDouble(),
    exactValue = record.count.toBigDecimal(),
    spec = MobileQuantitySpec(
        profile = HealthConnectContract.MOBILE_WHEELCHAIR_PUSH_COUNT_PROFILE,
        category = HealthConnectConverter.ACTIVITY_CATEGORY,
        codeSystem = HealthConnectContract.LOINC,
        code = "96502-0",
        display = "Number of wheelchair pushes per time period",
        unitCode = "{pushes}",
        unitDisplay = "pushes",
        valueDomain = HealthConnectContract.quantityValueDomains.getValue("wheelchair-push-count"),
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertRestingHeartRate(
    record: RestingHeartRateRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion {
    if (record.beatsPerMinute < 0L) {
        throw InvalidHealthConnectRecord("RestingHeartRateRecord beats per minute must be unsigned.")
    }
    val source = sourceIdentity(record.metadata, RecordType.restingHeartRate.identifier)
    val resolvedContext = context.resolve(
        record.metadata,
        synchronizationScope.identityKey,
        bundleIdentifier(eventSequence),
    )
    val observation = baseObservation(
        record.metadata,
        source,
        null,
        resolvedContext,
        "resting-heart-rate",
    ).apply {
        claimMeasurementProfile(HealthConnectContract.MOBILE_RESTING_HEART_RATE_PROFILE)
        code = concept(
            HealthConnectContract.LOINC,
            "40443-4",
            "Resting heart rate",
        ).apply {
            addCoding(Coding(HealthConnectContract.LOINC, "8867-4", "Heart rate"))
        }
        addCategory(category(HealthConnectConverter.VITAL_SIGNS_CATEGORY))
        // Health Connect 1.1 exposes one instantaneous estimate and no aggregation window.
        effective = DateTimeType(record.time.fhirDateTime(record.zoneOffset, "Resting heart rate time"))
        value = quantity(record.beatsPerMinute.toBigDecimal(), "/min", "beats/minute")
    }
    return conversion(
        record.metadata,
        RecordType.restingHeartRate.identifier,
        source,
        listOf(observation),
        convertedAt,
        eventSequence,
        resolvedContext,
    )
}
