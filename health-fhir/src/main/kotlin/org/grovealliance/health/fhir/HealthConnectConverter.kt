//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseLap
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.records.metadata.Metadata
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.InstantType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Provenance
import org.hl7.fhir.r4.model.Quantity
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.Specimen
import org.hl7.fhir.r4.model.StringType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

/** Converts the explicitly supported Health Connect records into the Grove Mobile R4 contract. */
@OptIn(ExperimentalMindfulnessSessionApi::class)
@Suppress("CyclomaticComplexMethod", "LargeClass", "TooManyFunctions")
class HealthConnectConverter(
    private val context: HealthConnectConversionContext,
    internal val synchronizationScope: HealthConnectSynchronizationScope,
) {
    fun convert(record: Record, convertedAt: Instant, eventSequence: EventSequence): HealthConnectConversion {
        return convertWithEventToken(record, convertedAt, eventSequence)
    }

    internal fun preview(record: Record, convertedAt: Instant): HealthConnectConversion =
        // Output identities do not depend on the event sequence. Use a syntactically valid
        // provisional sequence so even the internal preview graph satisfies the exchange profile.
        convertWithEventToken(record, convertedAt, PREVIEW_EVENT_SEQUENCE)

    internal fun bundleIdentifier(sourceIdentifier: Identifier, eventSequence: EventSequence): Identifier =
        HealthConnectIdentity.exchange(
            context.graphIdentifierSystem,
            HealthConnectIdentity.repositoryScopeOf(sourceIdentifier),
            eventSequence,
        )

    private fun convertWithEventToken(
        record: Record,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion {
        HealthConnectWireFormat.requireFhirInstant(convertedAt, "Conversion event time")
        return when (record) {
            is ActiveCaloriesBurnedRecord -> convertActiveEnergy(record, convertedAt, eventSequence)
            is BasalBodyTemperatureRecord -> convertBasalBodyTemperature(record, convertedAt, eventSequence)
            is BasalMetabolicRateRecord -> convertBasalMetabolicRate(record, convertedAt, eventSequence)
            is BloodGlucoseRecord -> convertBloodGlucose(record, convertedAt, eventSequence)
            is BloodPressureRecord -> convertBloodPressure(record, convertedAt, eventSequence)
            is BodyFatRecord -> convertBodyFat(record, convertedAt, eventSequence)
            is BodyTemperatureRecord -> convertBodyTemperature(record, convertedAt, eventSequence)
            is BodyWaterMassRecord -> convertBodyWaterMass(record, convertedAt, eventSequence)
            is BoneMassRecord -> convertBoneMass(record, convertedAt, eventSequence)
            is CervicalMucusRecord -> convertCervicalMucus(record, convertedAt, eventSequence)
            is CyclingPedalingCadenceRecord -> convertCyclingCadence(record, convertedAt, eventSequence)
            is DistanceRecord -> convertDistance(record, convertedAt, eventSequence)
            is ElevationGainedRecord -> convertElevationGained(record, convertedAt, eventSequence)
            is ExerciseSessionRecord -> convertExerciseSession(record, convertedAt, eventSequence)
            is FloorsClimbedRecord -> convertFloorsClimbed(record, convertedAt, eventSequence)
            is HeartRateRecord -> convertHeartRate(record, convertedAt, eventSequence)
            is HeartRateVariabilityRmssdRecord ->
                convertHeartRateVariabilityRmssd(record, convertedAt, eventSequence)
            is HeightRecord -> convertHeight(record, convertedAt, eventSequence)
            is HydrationRecord -> convertHydration(record, convertedAt, eventSequence)
            is IntermenstrualBleedingRecord -> convertIntermenstrualBleeding(record, convertedAt, eventSequence)
            is LeanBodyMassRecord -> convertLeanBodyMass(record, convertedAt, eventSequence)
            is MenstruationFlowRecord -> convertMenstruationFlow(record, convertedAt, eventSequence)
            is MenstruationPeriodRecord -> convertMenstruationPeriod(record, convertedAt, eventSequence)
            is MindfulnessSessionRecord -> convertMindfulnessSession(record, convertedAt, eventSequence)
            is NutritionRecord -> convertNutrition(record, convertedAt, eventSequence)
            is OvulationTestRecord -> convertOvulationTest(record, convertedAt, eventSequence)
            is OxygenSaturationRecord -> convertOxygenSaturation(record, convertedAt, eventSequence)
            is PowerRecord -> convertPower(record, convertedAt, eventSequence)
            is RespiratoryRateRecord -> convertRespiratoryRate(record, convertedAt, eventSequence)
            is RestingHeartRateRecord -> convertRestingHeartRate(record, convertedAt, eventSequence)
            is SexualActivityRecord -> convertSexualActivity(record, convertedAt, eventSequence)
            is SkinTemperatureRecord -> convertSkinTemperature(record, convertedAt, eventSequence)
            is SleepSessionRecord -> convertSleepDuration(record, convertedAt, eventSequence)
            is SpeedRecord -> convertSpeed(record, convertedAt, eventSequence)
            is StepsCadenceRecord -> convertStepCadence(record, convertedAt, eventSequence)
            is StepsRecord -> convertSteps(record, convertedAt, eventSequence)
            is TotalCaloriesBurnedRecord -> convertTotalEnergy(record, convertedAt, eventSequence)
            is Vo2MaxRecord -> convertVo2Max(record, convertedAt, eventSequence)
            is WeightRecord -> convertWeight(record, convertedAt, eventSequence)
            is WheelchairPushesRecord -> convertWheelchairPushes(record, convertedAt, eventSequence)
            else -> throw UnsupportedHealthConnectRecord(record::class.qualifiedName ?: record::class.java.name)
        }
    }

    private fun convertActiveEnergy(
        record: ActiveCaloriesBurnedRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertIntervalQuantity(
        metadata = record.metadata,
        recordType = ACTIVE_CALORIES_BURNED_RECORD,
        start = record.startTime,
        startOffset = record.startZoneOffset,
        end = record.endTime,
        endOffset = record.endZoneOffset,
        value = record.energy.inKilocalories,
        spec = MobileQuantitySpec(
            profile = HealthConnectContract.MOBILE_ACTIVE_ENERGY_PROFILE,
            category = ACTIVITY_CATEGORY,
            codeSystem = HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
            code = "active-energy-burned",
            display = "Active energy burned",
            unitCode = "kcal",
            unitDisplay = "kcal",
        ),
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )

    private fun convertBasalMetabolicRate(
        record: BasalMetabolicRateRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertInstantQuantity(
        metadata = record.metadata,
        recordType = BASAL_METABOLIC_RATE_RECORD,
        time = record.time,
        offset = record.zoneOffset,
        value = record.basalMetabolicRate.inKilocaloriesPerDay,
        spec = MobileQuantitySpec(
            profile = HealthConnectContract.HEALTH_CONNECT_BASAL_METABOLIC_RATE_PROFILE,
            category = ACTIVITY_CATEGORY,
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

    private fun convertBodyFat(
        record: BodyFatRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertInstantQuantity(
        metadata = record.metadata,
        recordType = BODY_FAT_RECORD,
        time = record.time,
        offset = record.zoneOffset,
        value = record.percentage.value,
        spec = MobileQuantitySpec(
            profile = HealthConnectContract.MOBILE_BODY_FAT_PERCENTAGE_PROFILE,
            category = VITAL_SIGNS_CATEGORY,
            codeSystem = HealthConnectContract.LOINC,
            code = "41982-0",
            display = "Percentage of body fat Measured",
            unitCode = "%",
            unitDisplay = "%",
        ),
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )

    private fun convertBodyWaterMass(
        record: BodyWaterMassRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertInstantQuantity(
        metadata = record.metadata,
        recordType = BODY_WATER_MASS_RECORD,
        time = record.time,
        offset = record.zoneOffset,
        value = record.mass.inKilograms,
        spec = MobileQuantitySpec(
            profile = HealthConnectContract.MOBILE_BODY_WATER_MASS_PROFILE,
            category = VITAL_SIGNS_CATEGORY,
            codeSystem = HealthConnectContract.LOINC,
            code = "101683-1",
            display = "Body water mass",
            unitCode = "kg",
            unitDisplay = "kg",
        ),
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )

    private fun convertBoneMass(
        record: BoneMassRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertInstantQuantity(
        metadata = record.metadata,
        recordType = BONE_MASS_RECORD,
        time = record.time,
        offset = record.zoneOffset,
        value = record.mass.inKilograms,
        spec = MobileQuantitySpec(
            profile = HealthConnectContract.MOBILE_BONE_MASS_PROFILE,
            category = VITAL_SIGNS_CATEGORY,
            codeSystem = HealthConnectContract.LOINC,
            code = "101685-6",
            display = "Body bone mass",
            unitCode = "kg",
            unitDisplay = "kg",
        ),
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )

    private fun convertLeanBodyMass(
        record: LeanBodyMassRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertInstantQuantity(
        metadata = record.metadata,
        recordType = LEAN_BODY_MASS_RECORD,
        time = record.time,
        offset = record.zoneOffset,
        value = record.mass.inKilograms,
        spec = MobileQuantitySpec(
            profile = HealthConnectContract.MOBILE_LEAN_BODY_MASS_PROFILE,
            category = VITAL_SIGNS_CATEGORY,
            codeSystem = HealthConnectContract.LOINC,
            code = "91557-9",
            display = "Lean body weight",
            unitCode = "kg",
            unitDisplay = "kg",
        ),
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )

    private fun convertHeartRateVariabilityRmssd(
        record: HeartRateVariabilityRmssdRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertInstantQuantity(
        metadata = record.metadata,
        recordType = HEART_RATE_VARIABILITY_RMSSD_RECORD,
        time = record.time,
        offset = record.zoneOffset,
        value = record.heartRateVariabilityMillis,
        spec = MobileQuantitySpec(
            profile = HealthConnectContract.MOBILE_HEART_RATE_VARIABILITY_RMSSD_PROFILE,
            category = VITAL_SIGNS_CATEGORY,
            codeSystem = HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
            code = "heart-rate-variability-rmssd",
            display = "Heart rate variability RMSSD",
            unitCode = "ms",
            unitDisplay = "ms",
        ),
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )

    private fun convertVo2Max(
        record: Vo2MaxRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertInstantQuantity(
        metadata = record.metadata,
        recordType = VO2_MAX_RECORD,
        time = record.time,
        offset = record.zoneOffset,
        value = record.vo2MillilitersPerMinuteKilogram,
        spec = MobileQuantitySpec(
            profile = HealthConnectContract.MOBILE_VO2_MAX_PROFILE,
            category = ACTIVITY_CATEGORY,
            codeSystem = HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
            code = "vo2-max",
            display = "VO2 max",
            unitCode = "mL/kg/min",
            unitDisplay = "mL/kg/min",
        ),
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )

    private fun convertHydration(
        record: HydrationRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertIntervalQuantity(
        metadata = record.metadata,
        recordType = HYDRATION_RECORD,
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

    private fun convertElevationGained(
        record: ElevationGainedRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertIntervalQuantity(
        metadata = record.metadata,
        recordType = ELEVATION_GAINED_RECORD,
        start = record.startTime,
        startOffset = record.startZoneOffset,
        end = record.endTime,
        endOffset = record.endZoneOffset,
        value = record.elevation.inMeters,
        spec = MobileQuantitySpec(
            profile = HealthConnectContract.HEALTH_CONNECT_ELEVATION_GAINED_PROFILE,
            category = ACTIVITY_CATEGORY,
            codeSystem = HealthConnectContract.HEALTH_CONNECT_MEASUREMENT,
            code = "elevation-gained",
            display = "Elevation gained",
            unitCode = "m",
            unitDisplay = "m",
            // Health Connect admits negative elevation change over an interval.
            minimum = null,
            adapterSpecific = true,
        ),
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )

    private fun convertFloorsClimbed(
        record: FloorsClimbedRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertIntervalQuantity(
        metadata = record.metadata,
        recordType = FLOORS_CLIMBED_RECORD,
        start = record.startTime,
        startOffset = record.startZoneOffset,
        end = record.endTime,
        endOffset = record.endZoneOffset,
        value = record.floors,
        spec = MobileQuantitySpec(
            profile = HealthConnectContract.MOBILE_FLIGHTS_CLIMBED_PROFILE,
            category = ACTIVITY_CATEGORY,
            codeSystem = HealthConnectContract.LOINC,
            code = "100304-5",
            display = "Flights climbed [#] Reporting Period",
            unitCode = "{flights}",
            unitDisplay = "flights",
        ),
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )

    private fun convertTotalEnergy(
        record: TotalCaloriesBurnedRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertIntervalQuantity(
        metadata = record.metadata,
        recordType = TOTAL_CALORIES_BURNED_RECORD,
        start = record.startTime,
        startOffset = record.startZoneOffset,
        end = record.endTime,
        endOffset = record.endZoneOffset,
        value = record.energy.inKilocalories,
        spec = MobileQuantitySpec(
            profile = HealthConnectContract.HEALTH_CONNECT_TOTAL_ENERGY_PROFILE,
            category = ACTIVITY_CATEGORY,
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

    private fun convertWheelchairPushes(
        record: WheelchairPushesRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion {
        if (record.count !in 0..MAX_WHEELCHAIR_PUSH_COUNT) {
            throw InvalidHealthConnectRecord("WheelchairPushesRecord count must be between 0 and 1,000,000.")
        }
        return convertIntervalQuantity(
            metadata = record.metadata,
            recordType = WHEELCHAIR_PUSHES_RECORD,
            start = record.startTime,
            startOffset = record.startZoneOffset,
            end = record.endTime,
            endOffset = record.endZoneOffset,
            value = record.count.toDouble(),
            exactValue = record.count.toBigDecimal(),
            spec = MobileQuantitySpec(
                profile = HealthConnectContract.MOBILE_WHEELCHAIR_PUSH_COUNT_PROFILE,
                category = ACTIVITY_CATEGORY,
                codeSystem = HealthConnectContract.LOINC,
                code = "96502-0",
                display = "Number of wheelchair pushes per time period",
                unitCode = "{pushes}",
                unitDisplay = "pushes",
            ),
            convertedAt = convertedAt,
            eventSequence = eventSequence,
        )
    }

    private fun convertRestingHeartRate(
        record: RestingHeartRateRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion {
        if (record.beatsPerMinute < 0L) {
            throw InvalidHealthConnectRecord("RestingHeartRateRecord beats per minute must be unsigned.")
        }
        val source = sourceIdentity(record.metadata, RESTING_HEART_RATE_RECORD)
        val resolvedContext = context.resolve(record.metadata)
        val observation = baseObservation(
            record.metadata,
            source,
            null,
            resolvedContext,
        ).apply {
            claimMeasurementProfile(HealthConnectContract.MOBILE_RESTING_HEART_RATE_PROFILE)
            code = concept(
                HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
                "resting-heart-rate",
                "Resting heart rate",
            )
            // Health Connect states only the estimate's instant, not its daily estimation window.
            effective = Period().apply {
                startElement = DateTimeType(record.time.fhirDateTime(record.zoneOffset, "Resting heart rate time"))
                endElement = DateTimeType(record.time.fhirDateTime(record.zoneOffset, "Resting heart rate time"))
            }
            method = concept(HealthConnectContract.GROVE_AGGREGATION_METHOD, "daily-mean", "Daily mean")
            value = quantity(record.beatsPerMinute.toBigDecimal(), "/min", "beats/minute")
        }
        return conversion(
            record.metadata,
            RESTING_HEART_RATE_RECORD,
            source,
            listOf(observation),
            convertedAt,
            eventSequence,
            resolvedContext,
        )
    }

    private fun convertCyclingCadence(
        record: CyclingPedalingCadenceRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertSampleSeries(
        metadata = record.metadata,
        recordType = CYCLING_PEDALING_CADENCE_RECORD,
        start = record.startTime,
        end = record.endTime,
        samples = record.samples.map { SeriesSample(it.time, it.revolutionsPerMinute) },
        spec = MobileQuantitySpec(
            profile = HealthConnectContract.MOBILE_CYCLING_CADENCE_PROFILE,
            category = ACTIVITY_CATEGORY,
            codeSystem = HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
            code = "cycling-cadence",
            display = "Cycling cadence",
            unitCode = "/min",
            unitDisplay = "revolutions/minute",
        ),
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )

    private fun convertPower(
        record: PowerRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertSampleSeries(
        metadata = record.metadata,
        recordType = POWER_RECORD,
        start = record.startTime,
        end = record.endTime,
        samples = record.samples.map { SeriesSample(it.time, it.power.inWatts) },
        spec = MobileQuantitySpec(
            profile = HealthConnectContract.MOBILE_POWER_PROFILE,
            category = ACTIVITY_CATEGORY,
            codeSystem = HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
            code = "power",
            display = "Power",
            unitCode = "W",
            unitDisplay = "W",
        ),
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )

    private fun convertSpeed(
        record: SpeedRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertSampleSeries(
        metadata = record.metadata,
        recordType = SPEED_RECORD,
        start = record.startTime,
        end = record.endTime,
        samples = record.samples.map { SeriesSample(it.time, it.speed.inMetersPerSecond) },
        spec = MobileQuantitySpec(
            profile = HealthConnectContract.MOBILE_SPEED_PROFILE,
            category = ACTIVITY_CATEGORY,
            codeSystem = HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
            code = "speed",
            display = "Speed",
            unitCode = "m/s",
            unitDisplay = "m/s",
        ),
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )

    private fun convertStepCadence(
        record: StepsCadenceRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertSampleSeries(
        metadata = record.metadata,
        recordType = STEPS_CADENCE_RECORD,
        start = record.startTime,
        end = record.endTime,
        samples = record.samples.map { SeriesSample(it.time, it.rate) },
        spec = MobileQuantitySpec(
            profile = HealthConnectContract.HEALTH_CONNECT_STEP_CADENCE_PROFILE,
            category = ACTIVITY_CATEGORY,
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

    private fun convertSkinTemperature(
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
            recordType = SKIN_TEMPERATURE_RECORD,
            start = record.startTime,
            end = record.endTime,
            samples = record.deltas.map { delta ->
                SeriesSample(delta.time, requireNotNull(baseline).inCelsius + delta.delta.inCelsius)
            },
            spec = MobileQuantitySpec(
                profile = HealthConnectContract.MOBILE_SKIN_TEMPERATURE_PROFILE,
                category = VITAL_SIGNS_CATEGORY,
                codeSystem = HealthConnectContract.LOINC,
                code = "61008-9",
                display = "Body surface temperature",
                unitCode = "Cel",
                unitDisplay = "Cel",
                minimum = null,
            ),
            bodySite = skinTemperatureMeasurementLocation(record.measurementLocation),
            convertedAt = convertedAt,
            eventSequence = eventSequence,
        )
    }

    private fun convertMenstruationFlow(
        record: MenstruationFlowRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertInstantCoded(
        metadata = record.metadata,
        recordType = MENSTRUATION_FLOW_RECORD,
        time = record.time,
        offset = record.zoneOffset,
        spec = MobileCodedSpec(
            profile = HealthConnectContract.MOBILE_MENSTRUATION_FLOW_PROFILE,
            category = null,
            code = "menstruation-flow",
            display = "Menstruation flow",
        ),
        value = codedValue(
            HealthConnectContract.GROVE_MENSTRUATION_FLOW,
            HealthConnectContract.HEALTH_CONNECT_MENSTRUATION_FLOW,
            menstruationFlowCoding(record.flow),
        ),
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )

    private fun convertOvulationTest(
        record: OvulationTestRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertInstantCoded(
        metadata = record.metadata,
        recordType = OVULATION_TEST_RECORD,
        time = record.time,
        offset = record.zoneOffset,
        spec = MobileCodedSpec(
            profile = HealthConnectContract.MOBILE_OVULATION_TEST_RESULT_PROFILE,
            category = null,
            code = "ovulation-test-result",
            display = "Ovulation test result",
        ),
        value = codedValue(
            HealthConnectContract.GROVE_OVULATION_TEST_RESULT,
            HealthConnectContract.HEALTH_CONNECT_OVULATION_TEST_RESULT,
            ovulationTestCoding(record.result),
        ),
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )

    private fun convertSexualActivity(
        record: SexualActivityRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertInstantCoded(
        metadata = record.metadata,
        recordType = SEXUAL_ACTIVITY_RECORD,
        time = record.time,
        offset = record.zoneOffset,
        spec = MobileCodedSpec(
            profile = HealthConnectContract.MOBILE_SEXUAL_ACTIVITY_PROFILE,
            category = null,
            code = "sexual-activity",
            display = "Sexual activity",
        ),
        value = codedValue(
            HealthConnectContract.GROVE_SEXUAL_ACTIVITY,
            HealthConnectContract.HEALTH_CONNECT_SEXUAL_ACTIVITY_PROTECTION,
            sexualActivityCoding(record.protectionUsed),
        ),
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )

    private fun convertCervicalMucus(
        record: CervicalMucusRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion {
        val component = cervicalMucusSensationCoding(record.sensation)?.let { sensation ->
            Observation.ObservationComponentComponent().apply {
                code = concept(
                    HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
                    "cervical-mucus-sensation",
                    "Cervical mucus sensation",
                )
                value = codedValue(
                    HealthConnectContract.GROVE_CERVICAL_MUCUS_SENSATION,
                    HealthConnectContract.HEALTH_CONNECT_CERVICAL_MUCUS_SENSATION,
                    sensation,
                )
            }
        }
        return convertInstantCoded(
            metadata = record.metadata,
            recordType = CERVICAL_MUCUS_RECORD,
            time = record.time,
            offset = record.zoneOffset,
            spec = MobileCodedSpec(
                profile = HealthConnectContract.MOBILE_CERVICAL_MUCUS_QUALITY_PROFILE,
                category = null,
                code = "cervical-mucus-quality",
                display = "Cervical mucus quality",
            ),
            value = codedValue(
                HealthConnectContract.GROVE_CERVICAL_MUCUS_QUALITY,
                HealthConnectContract.HEALTH_CONNECT_CERVICAL_MUCUS_APPEARANCE,
                cervicalMucusAppearanceCoding(record.appearance),
            ),
            component = component,
            convertedAt = convertedAt,
            eventSequence = eventSequence,
        )
    }

    private fun convertIntermenstrualBleeding(
        record: IntermenstrualBleedingRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertInstantCoded(
        metadata = record.metadata,
        recordType = INTERMENSTRUAL_BLEEDING_RECORD,
        time = record.time,
        offset = record.zoneOffset,
        spec = MobileCodedSpec(
            profile = HealthConnectContract.MOBILE_INTERMENSTRUAL_BLEEDING_PROFILE,
            category = null,
            code = "intermenstrual-bleeding",
            display = "Intermenstrual bleeding",
        ),
        value = CodeableConcept(
            Coding(HealthConnectContract.GROVE_INTERMENSTRUAL_BLEEDING, "present", "Present"),
        ),
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )

    private fun convertMenstruationPeriod(
        record: MenstruationPeriodRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertPeriodCoded(
        metadata = record.metadata,
        recordType = MENSTRUATION_PERIOD_RECORD,
        start = record.startTime,
        startOffset = record.startZoneOffset,
        end = record.endTime,
        endOffset = record.endZoneOffset,
        spec = MobileCodedSpec(
            profile = HealthConnectContract.HEALTH_CONNECT_MENSTRUATION_PERIOD_PROFILE,
            category = null,
            code = "menstruation-period",
            display = "Menstruation period",
            codeSystem = HealthConnectContract.HEALTH_CONNECT_MEASUREMENT,
            adapterSpecific = true,
        ),
        value = CodeableConcept(
            Coding(HealthConnectContract.HEALTH_CONNECT_MENSTRUATION_PERIOD, "present", "Present"),
        ),
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )

    private fun convertMindfulnessSession(
        record: MindfulnessSessionRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion {
        val nanos = ChronoUnit.NANOS.between(record.startTime, record.endTime)
        if (nanos <= 0L) {
            throw InvalidHealthConnectRecord("MindfulnessSessionRecord must have a positive interval.")
        }
        val minutes = BigDecimal.valueOf(nanos)
            .divide(NANOSECONDS_PER_MINUTE, SESSION_DURATION_SCALE, RoundingMode.HALF_EVEN)
            .stripTrailingZeros()
            .withPlainScale()
        val source = sourceIdentity(record.metadata, MINDFULNESS_SESSION_RECORD)
        val resolvedContext = context.resolve(record.metadata)
        val observation = baseObservation(
            record.metadata,
            source,
            null,
            resolvedContext,
        ).apply {
            claimMeasurementProfile(HealthConnectContract.MOBILE_MINDFULNESS_SESSION_PROFILE)
            addCategory(category(ACTIVITY_CATEGORY))
            code = concept(
                HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
                "mindfulness-session-duration",
                "Mindfulness session duration",
            )
            effective = Period().apply {
                startElement = DateTimeType(
                    record.startTime.fhirDateTime(record.startZoneOffset, "Mindfulness start time"),
                )
                endElement = DateTimeType(
                    record.endTime.fhirDateTime(record.endZoneOffset, "Mindfulness end time"),
                )
            }
            value = quantity(minutes, "min", "min")
        }
        return conversion(
            record.metadata,
            MINDFULNESS_SESSION_RECORD,
            source,
            listOf(observation),
            convertedAt,
            eventSequence,
            resolvedContext,
        )
    }

    private fun convertNutrition(
        record: NutritionRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion {
        if (!record.startTime.isBefore(record.endTime)) {
            throw InvalidHealthConnectRecord("NutritionRecord must have a positive interval.")
        }
        val source = sourceIdentity(record.metadata, NUTRITION_RECORD)
        val resolvedContext = context.resolve(record.metadata)
        val observations = NUTRIENT_OUTPUTS.mapNotNull { nutrient ->
            val value = nutrient.extract(record) ?: return@mapNotNull null
            val decimal = value.fhirDecimal(nutrient.spec.display)
            baseObservation(
                record.metadata,
                source,
                HealthConnectIdentity.nutrientOutput(source, nutrient.measurement),
                resolvedContext,
            ).apply {
                claimProfile(nutrient.spec)
                code = concept(nutrient.spec.codeSystem, nutrient.spec.code, nutrient.spec.display)
                effective = Period().apply {
                    startElement = DateTimeType(
                        record.startTime.fhirDateTime(record.startZoneOffset, "Nutrition start time"),
                    )
                    endElement = DateTimeType(
                        record.endTime.fhirDateTime(record.endZoneOffset, "Nutrition end time"),
                    )
                }
                this.value = quantity(decimal, nutrient.spec.unitCode, nutrient.spec.unitDisplay)
            }
        }
        return conversion(
            record.metadata,
            NUTRITION_RECORD,
            source,
            observations,
            convertedAt,
            eventSequence,
            resolvedContext,
        )
    }

    private fun convertBloodGlucose(
        record: BloodGlucoseRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion {
        val source = sourceIdentity(record.metadata, BLOOD_GLUCOSE_RECORD)
        val resolvedContext = context.resolve(record.metadata)
        val definition = bloodGlucoseDefinition(record.specimenSource)
        val specimenIdentity = specimenIdentifier(source, definition.specimenSourceCode)
        val specimen = Specimen().apply {
            meta.addProfile(HealthConnectContract.HEALTH_CONNECT_SPECIMEN_PROFILE)
            addIdentifier(specimenIdentity.copy())
            status = Specimen.SpecimenStatus.AVAILABLE
            type = CodeableConcept(definition.specimenType.copy())
            subject = resolvedContext.subject.copy()
        }
        val specimenResource = HealthConnectBundleResource(specimenIdentity, specimen)
        val observation = baseObservation(
            record.metadata,
            source,
            null,
            resolvedContext,
        ).apply {
            claimAdapterSpecificProfile(definition.profile)
            addCategory(category(LABORATORY_CATEGORY))
            code = concept(HealthConnectContract.LOINC, definition.loinc, definition.loincDisplay)
            effective = DateTimeType(record.time.fhirDateTime(record.zoneOffset, "Blood-glucose time"))
            value = quantity(
                record.level.inMilligramsPerDeciliter.fhirDecimal("Blood-glucose value"),
                "mg/dL",
                "mg/dL",
            )
            this.specimen = specimenResource.reference()
            glucoseMealContext(record)?.let(::addExtension)
        }
        return conversion(
            record.metadata,
            BLOOD_GLUCOSE_RECORD,
            source,
            listOf(observation),
            convertedAt,
            eventSequence,
            resolvedContext,
            listOf(specimenResource),
        )
    }

    private fun convertBloodPressure(
        record: BloodPressureRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion {
        val source = sourceIdentity(record.metadata, BLOOD_PRESSURE_RECORD)
        val resolvedContext = context.resolve(record.metadata)
        val systolic = record.systolic.inMillimetersOfMercury.fhirDecimal("Blood-pressure systolic value")
        val diastolic = record.diastolic.inMillimetersOfMercury.fhirDecimal("Blood-pressure diastolic value")
        val observation = baseObservation(
            record.metadata,
            source,
            null,
            resolvedContext,
        ).apply {
            claimMeasurementProfile(HealthConnectContract.MOBILE_BLOOD_PRESSURE_PROFILE)
            addCategory(concept(HealthConnectContract.OBSERVATION_CATEGORY, VITAL_SIGNS_CATEGORY, "Vital Signs"))
            code = concept(HealthConnectContract.LOINC, "85354-9", "Blood pressure panel")
            effective = DateTimeType(record.time.fhirDateTime(record.zoneOffset, "Blood-pressure time"))
            addComponent().apply {
                code = concept(HealthConnectContract.LOINC, "8480-6", "Systolic blood pressure")
                value = quantity(systolic, "mm[Hg]", "mmHg")
            }
            bloodPressureBodyPosition(record.bodyPosition)?.let { position ->
                addExtension(
                    Extension(
                        HealthConnectContract.OBSERVATION_BODY_POSITION,
                        CodeableConcept(position),
                    ),
                )
            }
            bodySite = bloodPressureMeasurementLocation(record.measurementLocation)
            addComponent().apply {
                code = concept(HealthConnectContract.LOINC, "8462-4", "Diastolic blood pressure")
                value = quantity(diastolic, "mm[Hg]", "mmHg")
            }
        }
        return conversion(
            record.metadata,
            BLOOD_PRESSURE_RECORD,
            source,
            listOf(observation),
            convertedAt,
            eventSequence,
            resolvedContext,
        )
    }

    private fun convertBasalBodyTemperature(
        record: BasalBodyTemperatureRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertInstantQuantity(
        metadata = record.metadata,
        recordType = BASAL_BODY_TEMPERATURE_RECORD,
        time = record.time,
        offset = record.zoneOffset,
        value = record.temperature.inCelsius,
        spec = MobileQuantitySpec(
            profile = HealthConnectContract.MOBILE_BASAL_BODY_TEMPERATURE_PROFILE,
            category = VITAL_SIGNS_CATEGORY,
            codeSystem = HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
            code = "basal-body-temperature",
            display = "Basal body temperature",
            unitCode = "Cel",
            unitDisplay = "Cel",
            minimum = null,
        ),
        bodySite = temperatureMeasurementLocation(record.measurementLocation),
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )

    private fun convertBodyTemperature(
        record: BodyTemperatureRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertInstantQuantity(
        metadata = record.metadata,
        recordType = BODY_TEMPERATURE_RECORD,
        time = record.time,
        offset = record.zoneOffset,
        value = record.temperature.inCelsius,
        spec = MobileQuantitySpec(
            profile = HealthConnectContract.MOBILE_BODY_TEMPERATURE_PROFILE,
            category = VITAL_SIGNS_CATEGORY,
            codeSystem = HealthConnectContract.LOINC,
            code = "8310-5",
            display = "Body temperature",
            unitCode = "Cel",
            unitDisplay = "Cel",
            minimum = null,
        ),
        bodySite = temperatureMeasurementLocation(record.measurementLocation),
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )

    private fun convertDistance(
        record: DistanceRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertIntervalQuantity(
        metadata = record.metadata,
        recordType = DISTANCE_RECORD,
        start = record.startTime,
        startOffset = record.startZoneOffset,
        end = record.endTime,
        endOffset = record.endZoneOffset,
        value = record.distance.inMeters,
        spec = MobileQuantitySpec(
            profile = HealthConnectContract.MOBILE_DISTANCE_PROFILE,
            category = ACTIVITY_CATEGORY,
            codeSystem = HealthConnectContract.LOINC,
            code = "103208-5",
            display = "Distance traveled",
            unitCode = "m",
            unitDisplay = "m",
        ),
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )

    private fun convertHeight(
        record: HeightRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertInstantQuantity(
        metadata = record.metadata,
        recordType = HEIGHT_RECORD,
        time = record.time,
        offset = record.zoneOffset,
        value = record.height.inMeters * CENTIMETERS_PER_METER,
        spec = MobileQuantitySpec(
            profile = HealthConnectContract.MOBILE_BODY_HEIGHT_PROFILE,
            category = VITAL_SIGNS_CATEGORY,
            codeSystem = HealthConnectContract.LOINC,
            code = "8302-2",
            display = "Body height",
            unitCode = "cm",
            unitDisplay = "cm",
        ),
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )

    private fun convertOxygenSaturation(
        record: OxygenSaturationRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertInstantQuantity(
        metadata = record.metadata,
        recordType = OXYGEN_SATURATION_RECORD,
        time = record.time,
        offset = record.zoneOffset,
        value = record.percentage.value,
        spec = MobileQuantitySpec(
            profile = HealthConnectContract.MOBILE_OXYGEN_SATURATION_PROFILE,
            category = VITAL_SIGNS_CATEGORY,
            codeSystem = HealthConnectContract.LOINC,
            code = "2708-6",
            display = "Oxygen saturation in Arterial blood",
            unitCode = "%",
            unitDisplay = "%",
        ),
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )

    private fun convertRespiratoryRate(
        record: RespiratoryRateRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion = convertInstantQuantity(
        metadata = record.metadata,
        recordType = RESPIRATORY_RATE_RECORD,
        time = record.time,
        offset = record.zoneOffset,
        value = record.rate,
        spec = MobileQuantitySpec(
            profile = HealthConnectContract.MOBILE_RESPIRATORY_RATE_PROFILE,
            category = VITAL_SIGNS_CATEGORY,
            codeSystem = HealthConnectContract.LOINC,
            code = "9279-1",
            display = "Respiratory rate",
            unitCode = "/min",
            unitDisplay = "breaths/minute",
        ),
        convertedAt = convertedAt,
        eventSequence = eventSequence,
    )

    @Suppress("LongMethod")
    private fun convertSleepDuration(
        record: SleepSessionRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion {
        val nanos = ChronoUnit.NANOS.between(record.startTime, record.endTime)
        if (nanos <= 0L) throw InvalidHealthConnectRecord("SleepSessionRecord must have a positive interval.")
        val hours = BigDecimal.valueOf(nanos)
            .divide(NANOSECONDS_PER_HOUR, SESSION_DURATION_SCALE, RoundingMode.HALF_EVEN)
            .stripTrailingZeros()
            .withPlainScale()
        val source = sourceIdentity(record.metadata, SLEEP_SESSION_RECORD)
        val resolvedContext = context.resolve(record.metadata)
        val occurrences = mutableMapOf<Triple<Instant, Instant, Int>, Int>()
        val stages = record.stages
            .sortedWith(compareBy<SleepSessionRecord.Stage>({ it.startTime }, { it.endTime }, { it.stage }))
            .map { stage ->
                if (
                    !stage.startTime.isBefore(stage.endTime) ||
                    stage.startTime < record.startTime ||
                    stage.endTime > record.endTime
                ) {
                    throw InvalidHealthConnectRecord(
                        "Every SleepSessionRecord stage must be a positive interval inside its session.",
                    )
                }
                val occurrenceKey = Triple(stage.startTime, stage.endTime, stage.stage)
                val occurrence = occurrences.getOrDefault(occurrenceKey, 0)
                occurrences[occurrenceKey] = occurrence + 1
                val stageIdentity = sleepStageIdentifier(source, stage, occurrence)
                val stageCoding = sleepStageCoding(stage.stage)
                baseObservation(record.metadata, source, stageIdentity, resolvedContext).apply {
                    claimMeasurementProfile(HealthConnectContract.MOBILE_SLEEP_STAGE_PROFILE)
                    addCategory(category(ACTIVITY_CATEGORY))
                    code = concept(
                        HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
                        "sleep-stage",
                        "Sleep stage",
                    )
                    effective = Period().apply {
                        startElement = DateTimeType(
                            stage.startTime.fhirDateTime(null, "Sleep-stage start time"),
                        )
                        endElement = DateTimeType(
                            stage.endTime.fhirDateTime(null, "Sleep-stage end time"),
                        )
                    }
                    value = CodeableConcept().apply {
                        addCoding(
                            Coding(
                                HealthConnectContract.GROVE_SLEEP_STAGE,
                                stageCoding.sharedCode,
                                stageCoding.sharedDisplay,
                            ),
                        )
                        addCoding(
                            Coding(
                                HealthConnectContract.HEALTH_CONNECT_SLEEP_STAGE,
                                stageCoding.sourceCode,
                                stageCoding.sourceDisplay,
                            ),
                        )
                    }
                }
            }
        val summary = baseObservation(
            record.metadata,
            source,
            null,
            resolvedContext,
        ).apply {
            claimMeasurementProfile(HealthConnectContract.MOBILE_SLEEP_DURATION_PROFILE)
            addCategory(category(ACTIVITY_CATEGORY))
            code = concept(HealthConnectContract.LOINC, "93832-4", "Sleep duration")
            effective = Period().apply {
                startElement = DateTimeType(record.startTime.fhirDateTime(record.startZoneOffset, "Sleep start time"))
                endElement = DateTimeType(record.endTime.fhirDateTime(record.endZoneOffset, "Sleep end time"))
            }
            value = quantity(hours, "h", "h")
            stages.forEach { stage ->
                addHasMember(Reference(GroveExchangeIdentity.fullUrl(outputIdentifier(stage))))
            }
            record.title?.takeIf { it.isNotBlank() }?.let { title ->
                addExtension(
                    Extension(HealthConnectContract.HEALTH_CONNECT_SLEEP_TITLE, StringType(title)),
                )
            }
            record.notes?.takeIf { it.isNotBlank() }?.let { addNote().text = it }
        }
        return conversion(
            record.metadata,
            SLEEP_SESSION_RECORD,
            source,
            listOf(summary) + stages,
            convertedAt,
            eventSequence,
            resolvedContext,
        )
    }

    private fun convertExerciseSession(
        record: ExerciseSessionRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion {
        if (!record.startTime.isBefore(record.endTime)) {
            throw InvalidHealthConnectRecord("ExerciseSessionRecord must have a positive interval.")
        }
        val source = sourceIdentity(record.metadata, EXERCISE_SESSION_RECORD)
        val resolvedContext = context.resolve(record.metadata)
        val children = workoutSegments(record, source, resolvedContext) +
            workoutLaps(record, source, resolvedContext)
        val summary = baseObservation(
            record.metadata,
            source,
            null,
            resolvedContext,
        ).apply {
            claimMeasurementProfile(HealthConnectContract.MOBILE_WORKOUT_PROFILE)
            addCategory(category(ACTIVITY_CATEGORY))
            code = concept(HealthConnectContract.GROVE_MOBILE_MEASUREMENT, "workout", "Workout session")
            effective = Period().apply {
                startElement = DateTimeType(
                    record.startTime.fhirDateTime(record.startZoneOffset, "Workout start time"),
                )
                endElement = DateTimeType(record.endTime.fhirDateTime(record.endZoneOffset, "Workout end time"))
            }
            value = codedValue(
                HealthConnectContract.GROVE_WORKOUT_ACTIVITY,
                HealthConnectContract.HEALTH_CONNECT_EXERCISE_TYPE,
                HealthConnectWorkoutVocabulary.activity(record.exerciseType),
            )
            children.forEach { child ->
                addHasMember(Reference(GroveExchangeIdentity.fullUrl(outputIdentifier(child))))
            }
            record.title?.takeIf { it.isNotBlank() }?.let { title ->
                addExtension(
                    Extension(HealthConnectContract.HEALTH_CONNECT_EXERCISE_TITLE, StringType(title)),
                )
            }
            record.notes?.takeIf { it.isNotBlank() }?.let { addNote().text = it }
        }
        return conversion(
            record.metadata,
            EXERCISE_SESSION_RECORD,
            source,
            listOf(summary) + children,
            convertedAt,
            eventSequence,
            resolvedContext,
        )
    }

    private fun workoutSegments(
        record: ExerciseSessionRecord,
        source: Identifier,
        resolvedContext: ResolvedFhirContext,
    ): List<Observation> {
        val occurrences = mutableMapOf<Triple<Instant, Instant, Int>, Int>()
        return record.segments
            .sortedWith(compareBy<ExerciseSegment>({ it.startTime }, { it.endTime }, { it.segmentType }))
            .map { segment ->
                requireWorkoutInterval(record, segment.startTime, segment.endTime)
                val occurrenceKey = Triple(segment.startTime, segment.endTime, segment.segmentType)
                val occurrence = occurrences.getOrDefault(occurrenceKey, 0)
                occurrences[occurrenceKey] = occurrence + 1
                val classification = HealthConnectWorkoutVocabulary.segment(segment.segmentType)
                workoutSegmentObservation(
                    record.metadata,
                    source,
                    resolvedContext,
                    segment.startTime,
                    segment.endTime,
                    occurrence,
                    classification,
                ).apply {
                    if (segment.repetitions > 0) {
                        addComponent().apply {
                            code = workoutStatistic("repetitions", "Repetitions")
                            value = quantity(segment.repetitions.toBigDecimal(), "{count}", "repetitions")
                        }
                    }
                }
            }
    }

    private fun workoutLaps(
        record: ExerciseSessionRecord,
        source: Identifier,
        resolvedContext: ResolvedFhirContext,
    ): List<Observation> {
        val occurrences = mutableMapOf<Pair<Instant, Instant>, Int>()
        return record.laps
            .sortedWith(compareBy<ExerciseLap>({ it.startTime }, { it.endTime }))
            .map { lap ->
                requireWorkoutInterval(record, lap.startTime, lap.endTime)
                val occurrenceKey = lap.startTime to lap.endTime
                val occurrence = occurrences.getOrDefault(occurrenceKey, 0)
                occurrences[occurrenceKey] = occurrence + 1
                workoutSegmentObservation(
                    record.metadata,
                    source,
                    resolvedContext,
                    lap.startTime,
                    lap.endTime,
                    occurrence,
                    HealthConnectWorkoutVocabulary.lap(),
                ).apply {
                    lap.length?.let { length ->
                        addComponent().apply {
                            code = workoutStatistic("lap-length", "Lap length")
                            value = quantity(length.inMeters.fhirDecimal("Workout lap length"), "m", "m")
                        }
                    }
                }
            }
    }

    @Suppress("LongParameterList")
    private fun workoutSegmentObservation(
        metadata: Metadata,
        source: Identifier,
        resolvedContext: ResolvedFhirContext,
        start: Instant,
        end: Instant,
        occurrence: Int,
        classification: WorkoutClassification,
    ): Observation {
        val identity = HealthConnectIdentity.segmentOutput(
            source,
            start,
            end,
            classification.value.sourceCode,
            occurrence,
        )
        return baseObservation(metadata, source, identity, resolvedContext).apply {
            claimMeasurementProfile(HealthConnectContract.MOBILE_WORKOUT_SEGMENT_PROFILE)
            addCategory(category(ACTIVITY_CATEGORY))
            code = concept(
                HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
                "workout-segment",
                "Workout segment",
            )
            // Health Connect supplies offsets for the containing session, not for each segment or lap.
            effective = Period().apply {
                startElement = DateTimeType(start.fhirDateTime(null, "Workout-segment start time"))
                endElement = DateTimeType(end.fhirDateTime(null, "Workout-segment end time"))
            }
            value = codedValue(
                classification.sharedSystem,
                HealthConnectContract.HEALTH_CONNECT_EXERCISE_SEGMENT_TYPE,
                classification.value,
            )
        }
    }

    private fun requireWorkoutInterval(record: ExerciseSessionRecord, start: Instant, end: Instant) {
        if (!start.isBefore(end) || start < record.startTime || end > record.endTime) {
            throw InvalidHealthConnectRecord(
                "Every ExerciseSessionRecord segment and lap must be a positive interval inside its session.",
            )
        }
    }

    private fun workoutStatistic(code: String, display: String): CodeableConcept =
        concept(HealthConnectContract.GROVE_WORKOUT_STATISTIC, code, display)

    private fun convertHeartRate(
        record: HeartRateRecord,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion {
        val source = sourceIdentity(record.metadata, HEART_RATE_RECORD)
        val resolvedContext = context.resolve(record.metadata)
        validate(record)

        val occurrences = mutableMapOf<Instant, Int>()
        val observations = record.samples
            .sortedWith(compareBy<HeartRateRecord.Sample>({ it.time }, { it.beatsPerMinute }))
            .map { sample ->
                HealthConnectWireFormat.requireFhirInstant(sample.time, "Heart-rate sample time")
                // The identity carries no measured value, so the occurrence counts within the
                // instant alone; keying it by value too would let two readings share an identifier.
                val occurrence = occurrences.getOrDefault(sample.time, 0)
                occurrences[sample.time] = occurrence + 1
                val output = sampleIdentifier(source, sample, occurrence)
                baseObservation(record.metadata, source, output, resolvedContext).apply {
                    claimMeasurementProfile(HealthConnectContract.MOBILE_HEART_RATE_PROFILE)
                    addCategory(concept(HealthConnectContract.OBSERVATION_CATEGORY, "vital-signs", "Vital Signs"))
                    code = concept(HealthConnectContract.LOINC, "8867-4", "Heart rate")
                    // Health Connect supplies offsets for the containing Record, not for each Sample.
                    effective = DateTimeType(sample.time.fhirDateTime(null, "Heart-rate sample time"))
                    value = quantity(sample.beatsPerMinute.toBigDecimal(), "/min", "beats/minute")
                }
            }
        return conversion(
            record.metadata,
            HEART_RATE_RECORD,
            source,
            observations,
            convertedAt,
            eventSequence,
            resolvedContext,
        )
    }

    private fun convertSteps(record: StepsRecord, convertedAt: Instant, eventSequence: EventSequence): HealthConnectConversion {
        val source = sourceIdentity(record.metadata, STEPS_RECORD)
        val resolvedContext = context.resolve(record.metadata)
        if (!record.startTime.isBefore(record.endTime)) {
            throw InvalidHealthConnectRecord("StepsRecord must have a positive interval.")
        }
        if (record.count !in 0..MAX_STEP_COUNT) {
            throw InvalidHealthConnectRecord("StepsRecord count must be between 0 and 1,000,000.")
        }
        val observation = baseObservation(
            record.metadata,
            source,
            null,
            resolvedContext,
        ).apply {
            claimMeasurementProfile(HealthConnectContract.MOBILE_STEP_COUNT_PROFILE)
            addCategory(concept(HealthConnectContract.OBSERVATION_CATEGORY, "activity", "Activity"))
            code = concept(
                HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
                "step-count-total",
                "Step count total",
            )
            effective = Period().apply {
                startElement = DateTimeType(record.startTime.fhirDateTime(record.startZoneOffset, "Steps start time"))
                endElement = DateTimeType(record.endTime.fhirDateTime(record.endZoneOffset, "Steps end time"))
            }
            value = quantity(record.count.toBigDecimal(), "{steps}", "steps")
        }
        return conversion(
            record.metadata,
            STEPS_RECORD,
            source,
            listOf(observation),
            convertedAt,
            eventSequence,
            resolvedContext,
        )
    }

    private fun convertWeight(record: WeightRecord, convertedAt: Instant, eventSequence: EventSequence): HealthConnectConversion {
        val source = sourceIdentity(record.metadata, WEIGHT_RECORD)
        val resolvedContext = context.resolve(record.metadata)
        val kilograms = record.weight.inKilograms
        if (!kilograms.isFinite() || kilograms < 0.0 || kilograms > MAX_WEIGHT_KILOGRAMS) {
            throw InvalidHealthConnectRecord("WeightRecord must contain a finite weight in [0, 1000] kg.")
        }
        val observation = baseObservation(
            record.metadata,
            source,
            null,
            resolvedContext,
        ).apply {
            claimMeasurementProfile(HealthConnectContract.MOBILE_BODY_WEIGHT_PROFILE)
            addCategory(concept(HealthConnectContract.OBSERVATION_CATEGORY, "vital-signs", "Vital Signs"))
            code = concept(HealthConnectContract.LOINC, "29463-7", "Body weight")
            effective = DateTimeType(record.time.fhirDateTime(record.zoneOffset, "Weight time"))
            value = quantity(BigDecimal.valueOf(kilograms), "kg", "kg")
        }
        return conversion(
            record.metadata,
            WEIGHT_RECORD,
            source,
            listOf(observation),
            convertedAt,
            eventSequence,
            resolvedContext,
        )
    }

    @Suppress("LongParameterList")
    private fun convertInstantQuantity(
        metadata: Metadata,
        recordType: String,
        time: Instant,
        offset: ZoneOffset?,
        value: Double,
        spec: MobileQuantitySpec,
        bodySite: CodeableConcept? = null,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion {
        val source = sourceIdentity(metadata, recordType)
        val resolvedContext = context.resolve(metadata)
        val decimal = value.fhirDecimal(spec.display, spec.minimum)
        val observation = baseObservation(
            metadata,
            source,
            null,
            resolvedContext,
        ).apply {
            claimProfile(spec)
            spec.category?.let { addCategory(category(it)) }
            code = concept(spec.codeSystem, spec.code, spec.display)
            effective = DateTimeType(time.fhirDateTime(offset, "${spec.display} time"))
            this.value = quantity(decimal, spec.unitCode, spec.unitDisplay)
            this.bodySite = bodySite
        }
        return conversion(
            metadata,
            recordType,
            source,
            listOf(observation),
            convertedAt,
            eventSequence,
            resolvedContext,
        )
    }

    @Suppress("LongParameterList")
    private fun convertIntervalQuantity(
        metadata: Metadata,
        recordType: String,
        start: Instant,
        startOffset: ZoneOffset?,
        end: Instant,
        endOffset: ZoneOffset?,
        value: Double,
        exactValue: BigDecimal? = null,
        spec: MobileQuantitySpec,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion {
        if (!start.isBefore(end)) throw InvalidHealthConnectRecord("$recordType must have a positive interval.")
        val source = sourceIdentity(metadata, recordType)
        val resolvedContext = context.resolve(metadata)
        val decimal = exactValue ?: value.fhirDecimal(spec.display, spec.minimum)
        val observation = baseObservation(
            metadata,
            source,
            null,
            resolvedContext,
        ).apply {
            claimProfile(spec)
            spec.category?.let { addCategory(category(it)) }
            code = concept(spec.codeSystem, spec.code, spec.display)
            effective = Period().apply {
                startElement = DateTimeType(start.fhirDateTime(startOffset, "${spec.display} start time"))
                endElement = DateTimeType(end.fhirDateTime(endOffset, "${spec.display} end time"))
            }
            this.value = quantity(decimal, spec.unitCode, spec.unitDisplay)
        }
        return conversion(
            metadata,
            recordType,
            source,
            listOf(observation),
            convertedAt,
            eventSequence,
            resolvedContext,
        )
    }

    @Suppress("LongParameterList")
    private fun convertSampleSeries(
        metadata: Metadata,
        recordType: String,
        start: Instant,
        end: Instant,
        samples: List<SeriesSample>,
        spec: MobileQuantitySpec,
        bodySite: CodeableConcept? = null,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion {
        if (start.isAfter(end)) {
            throw InvalidHealthConnectRecord("$recordType startTime must not be after endTime.")
        }
        if (samples.any { it.time < start || it.time > end }) {
            throw InvalidHealthConnectRecord("${spec.display} sample lies outside its source record interval.")
        }
        val source = sourceIdentity(metadata, recordType)
        val resolvedContext = context.resolve(metadata)
        val occurrences = mutableMapOf<Instant, Int>()
        val observations = samples
            .sortedWith(compareBy({ it.time }, { it.value }))
            .map { sample ->
                HealthConnectWireFormat.requireFhirInstant(sample.time, "${spec.display} sample time")
                val decimal = sample.value.fhirDecimal("${spec.display} sample value", spec.minimum)
                val valueToken = decimal.stripTrailingZeros().toPlainString()
                // The identity carries no measured value, so the occurrence counts within the
                // instant alone; keying it by value too would let two readings share an identifier.
                val occurrence = occurrences.getOrDefault(sample.time, 0)
                occurrences[sample.time] = occurrence + 1
                val output = HealthConnectIdentity.seriesSampleOutput(source, sample.time, occurrence)
                baseObservation(metadata, source, output, resolvedContext).apply {
                    claimProfile(spec)
                    spec.category?.let { addCategory(category(it)) }
                    code = concept(spec.codeSystem, spec.code, spec.display)
                    // Health Connect supplies offsets for the containing Record, not for each Sample.
                    effective = DateTimeType(sample.time.fhirDateTime(null, "${spec.display} sample time"))
                    value = quantity(decimal, spec.unitCode, spec.unitDisplay)
                    this.bodySite = bodySite?.copy()
                }
            }
        return conversion(
            metadata,
            recordType,
            source,
            observations,
            convertedAt,
            eventSequence,
            resolvedContext,
        )
    }

    private fun Observation.claimProfile(spec: MobileQuantitySpec) =
        claimProfile(spec.profile, spec.adapterSpecific)

    private fun Observation.claimProfile(spec: MobileCodedSpec) =
        claimProfile(spec.profile, spec.adapterSpecific)

    private fun Observation.claimProfile(profile: String, adapterSpecific: Boolean) {
        if (adapterSpecific) {
            claimAdapterSpecificProfile(profile)
        } else {
            claimMeasurementProfile(profile)
        }
    }

    @Suppress("LongParameterList")
    private fun convertInstantCoded(
        metadata: Metadata,
        recordType: String,
        time: Instant,
        offset: ZoneOffset?,
        spec: MobileCodedSpec,
        value: CodeableConcept,
        component: Observation.ObservationComponentComponent? = null,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion {
        val source = sourceIdentity(metadata, recordType)
        val resolvedContext = context.resolve(metadata)
        val observation = baseObservation(
            metadata,
            source,
            null,
            resolvedContext,
        ).apply {
            claimProfile(spec)
            spec.category?.let { addCategory(category(it)) }
            code = concept(spec.codeSystem, spec.code, spec.display)
            effective = DateTimeType(time.fhirDateTime(offset, "${spec.display} time"))
            this.value = value
            component?.let { addComponent(it) }
        }
        return conversion(
            metadata,
            recordType,
            source,
            listOf(observation),
            convertedAt,
            eventSequence,
            resolvedContext,
        )
    }

    @Suppress("LongParameterList")
    private fun convertPeriodCoded(
        metadata: Metadata,
        recordType: String,
        start: Instant,
        startOffset: ZoneOffset?,
        end: Instant,
        endOffset: ZoneOffset?,
        spec: MobileCodedSpec,
        value: CodeableConcept,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversion {
        if (!start.isBefore(end)) throw InvalidHealthConnectRecord("$recordType must have a positive interval.")
        val source = sourceIdentity(metadata, recordType)
        val resolvedContext = context.resolve(metadata)
        val observation = baseObservation(
            metadata,
            source,
            null,
            resolvedContext,
        ).apply {
            claimProfile(spec)
            spec.category?.let { addCategory(category(it)) }
            code = concept(spec.codeSystem, spec.code, spec.display)
            effective = Period().apply {
                startElement = DateTimeType(start.fhirDateTime(startOffset, "${spec.display} start time"))
                endElement = DateTimeType(end.fhirDateTime(endOffset, "${spec.display} end time"))
            }
            this.value = value
        }
        return conversion(
            metadata,
            recordType,
            source,
            listOf(observation),
            convertedAt,
            eventSequence,
            resolvedContext,
        )
    }

    private fun codedValue(sharedSystem: String, sourceSystem: String, coding: SourceCodedValue): CodeableConcept =
        CodeableConcept().apply {
            addCoding(Coding(sharedSystem, coding.sharedCode, coding.sharedDisplay))
            addCoding(Coding(sourceSystem, coding.sourceCode, coding.sourceDisplay))
        }

    private fun category(code: String): CodeableConcept = concept(
        HealthConnectContract.OBSERVATION_CATEGORY,
        code,
        when (code) {
            VITAL_SIGNS_CATEGORY -> "Vital Signs"
            LABORATORY_CATEGORY -> "Laboratory"
            ACTIVITY_CATEGORY -> "Activity"
            else -> code
        },
    )

    /** Keeps a stripped decimal in plain notation so a multiple of ten never gains an exponent. */
    private fun BigDecimal.withPlainScale(): BigDecimal = if (scale() < 0) setScale(0) else this

    private fun Double.fhirDecimal(field: String, minimum: Double? = 0.0): BigDecimal {
        if (!isFinite() || minimum != null && this < minimum) {
            val range = minimum?.let { "greater than or equal to $it" } ?: "finite"
            throw InvalidHealthConnectRecord("$field must be $range.")
        }
        return BigDecimal.valueOf(this)
    }

    /** Carries the writer's own identity for the record, when it assigns one. */
    private fun Observation.clientRecordIdentity(metadata: Metadata) {
        // A writer that re-imports a measurement reuses its clientRecordId and raises the version,
        // and the stored Record then carries a new metadata.id. Without this the same measurement
        // is counted twice; with it a receiver supersedes the lower version.
        val clientRecordId = metadata.clientRecordId?.takeIf { it.isNotBlank() } ?: return
        // A clientRecordId is unique only within the app that wrote it, so the writer is part of
        // the identity. Without it two apps that both chose "weighin-2026-08-19" would look like
        // one measurement, and a receiver applying the supersession rule would drop one of them.
        val writer = metadata.dataOrigin.packageName
        require(writer.isNotBlank() && '|' !in writer && '|' !in clientRecordId) {
            "A client record identity must not contain the writer separator."
        }
        addIdentifier(
            Identifier().apply {
                system = HealthConnectContract.WRITER_RECORD_IDENTIFIER
                value = "v1:$writer|$clientRecordId"
            },
        )
        addExtension(
            Extension(
                HealthConnectContract.WRITER_RECORD_VERSION,
                // The source version is a Long; narrowing it to a FHIR integer would wrap a
                // millisecond-based version into a negative number and invert the ordering.
                StringType(metadata.clientRecordVersion.toString()),
            ),
        )
    }

    private fun baseObservation(
        metadata: Metadata,
        sourceIdentifier: Identifier,
        outputIdentifier: Identifier?,
        resolvedContext: ResolvedFhirContext,
    ): Observation =
        Observation().apply {
            addIdentifier(sourceIdentifier.copy())
            outputIdentifier?.let { addIdentifier(it) }
            status = Observation.ObservationStatus.FINAL
            subject = resolvedContext.subject.copy()
            // lastModifiedTime, not the conversion instant: the emitted graph has to be identical
            // for an unchanged source version, or a re-read stops deduplicating and the outbox
            // replays it. The conversion event itself is recorded on Provenance.
            issuedElement = InstantType(metadata.lastModifiedTime.toString())
            clientRecordIdentity(metadata)

            recordingMethod(metadata)?.let { addExtension(it) }
            resolvedContext.recordingDevice?.let { device = it.copy() }
            resolvedContext.researchStudies.forEach { study ->
                addExtension(
                    Extension(
                        HealthConnectContract.RESEARCH_STUDY_EXTENSION,
                        study.copy(),
                    ),
                )
            }
        }

    private fun Observation.claimMeasurementProfile(sharedProfile: String) {
        check(meta.profile.isEmpty()) { "Measurement profile claims must be assigned exactly once." }
        meta.addProfile(sharedProfile)
        meta.addProfile(HealthConnectContract.HEALTH_CONNECT_OBSERVATION_PROFILE)
    }

    private fun Observation.claimAdapterSpecificProfile(adapterProfile: String) {
        check(adapterProfile in HealthConnectContract.adapterSpecificObservationProfiles) {
            "Only an admitted Health Connect-specific Observation profile may use this claim mode."
        }
        check(meta.profile.isEmpty()) { "Measurement profile claims must be assigned exactly once." }
        meta.addProfile(adapterProfile)
    }

    @Suppress("LongParameterList")
    private fun conversion(
        metadata: Metadata,
        recordType: String,
        source: Identifier,
        observations: List<Observation>,
        convertedAt: Instant,
        eventSequence: EventSequence,
        resolvedContext: ResolvedFhirContext,
        conversionResources: List<HealthConnectBundleResource<Resource>> = emptyList(),
    ): HealthConnectConversion {
        if (convertedAt < metadata.lastModifiedTime) {
            throw InvalidHealthConnectRecord(
                "The conversion event cannot precede the source version's lastModifiedTime.",
            )
        }
        attachRecordTypeLineage(observations, recordType)
        val provenance = observations.takeIf { it.isNotEmpty() }?.let {
            conversionProvenance(it, source, convertedAt, resolvedContext)
        }
        val bundle = bundle(
            source,
            observations,
            provenance,
            convertedAt,
            eventSequence,
            resolvedContext,
            conversionResources,
        )
        return HealthConnectConversion(
            conversionContractVersion = synchronizationScope.conversionContractVersion,
            sourceRecordIdentifier = source,
            sourceRecordType = recordType,
            sourceLastModified = metadata.lastModifiedTime,
            observations = observations,
            provenance = provenance,
            bundle = bundle,
        )
    }

    private fun attachRecordTypeLineage(observations: List<Observation>, recordType: String) {
        observations.forEach { observation ->
            check(
                observation.getExtensionsByUrl(
                    HealthConnectContract.HEALTH_CONNECT_RECORD_TYPE_EXTENSION,
                ).isEmpty(),
            ) { "Health Connect Record-type lineage must be assigned exactly once." }
            observation.addExtension(
                Extension(
                    HealthConnectContract.HEALTH_CONNECT_RECORD_TYPE_EXTENSION,
                    CodeType(recordType),
                ),
            )
        }
    }

    private fun conversionProvenance(
        outputs: List<Observation>,
        source: Identifier,
        convertedAt: Instant,
        resolvedContext: ResolvedFhirContext,
    ): Provenance = Provenance().apply {
        meta.addProfile(HealthConnectContract.HEALTH_CONNECT_PROVENANCE_PROFILE)
        occurred = DateTimeType(convertedAt.toString())
        recordedElement = InstantType(convertedAt.toString())
        activity = concept(
            HealthConnectContract.RECORD_LIFECYCLE,
            "transform",
            "Transform/Translate Record Lifecycle Event",
        )
        addAgent().apply {
            type = concept(HealthConnectContract.PROVENANCE_PARTICIPANT, "assembler", "Assembler")
            who = resolvedContext.assembler.copy()
        }
        outputs.forEach { observation ->
            addTarget(
                Reference().apply {
                    reference = GroveExchangeIdentity.fullUrl(outputIdentifier(observation))
                    type = "Observation"
                    identifier = outputIdentifier(observation).copy()
                },
            )
        }
        addEntity().apply {
            role = Provenance.ProvenanceEntityRole.SOURCE
            what = Reference().apply { identifier = source.copy() }
            addAgent().apply {
                type = concept(HealthConnectContract.PROVENANCE_PARTICIPANT, "enterer", "Enterer")
                who = resolvedContext.sourceApplication.copy()
            }
        }
    }

    @Suppress("LongParameterList")
    private fun bundle(
        source: Identifier,
        observations: List<Observation>,
        provenance: Provenance?,
        convertedAt: Instant,
        eventSequence: EventSequence,
        resolvedContext: ResolvedFhirContext,
        conversionResources: List<HealthConnectBundleResource<Resource>>,
    ): Bundle = Bundle().apply {
        identifier = bundleIdentifier(source, eventSequence)
        meta.addProfile(HealthConnectContract.MOBILE_EXCHANGE_BUNDLE_PROFILE)
        type = Bundle.BundleType.COLLECTION
        timestampElement = InstantType(convertedAt.toString())
        resolvedContext.resources.forEach { resolved ->
            addGroveEntry(resolved.entryIdentifier, resolved.resource.copy())
        }
        conversionResources.forEach { resolved ->
            addGroveEntry(resolved.entryIdentifier, resolved.resource.copy())
        }
        observations.sortedBy { outputIdentifier(it).value }.forEach { observation ->
            addGroveEntry(outputIdentifier(observation), observation.copy())
        }
        provenance?.let {
            addGroveEntry(
                HealthConnectIdentity.conversion(
                    context.graphIdentifierSystem,
                    HealthConnectIdentity.repositoryScopeOf(source),
                    eventSequence,
                ),
                it.copy(),
            )
        }
        check(entry.map { it.fullUrl }.distinct().size == entry.size) {
            "A Grove exchange Bundle cannot contain duplicate fullUrl values."
        }
    }

    private fun sourceIdentity(
        metadata: Metadata,
        recordTypeToken: String,
    ): Identifier {
        val packageName = metadata.dataOrigin.packageName
        val invalidReason = when {
            metadata.id.isBlank() ->
                "Health Connect metadata.id is absent; convert records only after reading them."
            packageName.isBlank() ->
                "Health Connect dataOrigin.packageName is absent."
            !metadata.lastModifiedTime.isAfter(Instant.EPOCH) ->
                "Health Connect lastModifiedTime must be a post-insertion instant after the Unix epoch."
            metadata.lastModifiedTime > HealthConnectWireFormat.MAX_FHIR_INSTANT ->
                "Health Connect lastModifiedTime must have a four-digit FHIR year no later than 9999."
            else -> null
        }
        invalidReason?.let { throw InvalidHealthConnectRecord(it) }
        return Identifier().apply {
            system = HealthConnectContract.HEALTH_CONNECT_RECORD_IDENTIFIER
            value = synchronizationScope.sourceRecordIdentifierValue(recordTypeToken, metadata.id)
        }
    }

    private fun validate(record: HeartRateRecord) {
        val invalidReason = when {
            record.startTime.isAfter(record.endTime) ->
                "HeartRateRecord startTime must not be after endTime."
            record.samples.any { it.time < record.startTime || it.time > record.endTime } ->
                "Heart-rate sample lies outside its source record interval."
            else -> null
        }
        invalidReason?.let { throw InvalidHealthConnectRecord(it) }
    }

    private fun sampleIdentifier(
        sourceIdentifier: Identifier,
        sample: HeartRateRecord.Sample,
        occurrence: Int,
    ): Identifier = HealthConnectIdentity.heartRateSampleOutput(sourceIdentifier, sample.time, occurrence)

    private fun sleepStageIdentifier(
        sourceIdentifier: Identifier,
        stage: SleepSessionRecord.Stage,
        occurrence: Int,
    ): Identifier = HealthConnectIdentity.sleepStageOutput(
        sourceIdentifier,
        stage.startTime,
        stage.endTime,
        sleepStageCoding(stage.stage).sourceCode,
        occurrence,
    )

    @Suppress("LongMethod")
    private fun bloodGlucoseDefinition(specimenSource: Int): BloodGlucoseDefinition = when (specimenSource) {
        BloodGlucoseRecord.SPECIMEN_SOURCE_WHOLE_BLOOD -> BloodGlucoseDefinition(
            profile = HealthConnectContract.HEALTH_CONNECT_WHOLE_BLOOD_GLUCOSE_PROFILE,
            loinc = "2339-0",
            loincDisplay = "Glucose [Mass/volume] in Blood",
            specimenSourceCode = "SPECIMEN_SOURCE_WHOLE_BLOOD",
            specimenType = Coding(
                HealthConnectContract.SNOMED_CT,
                "258580003",
                "Whole blood sample",
            ),
        )
        BloodGlucoseRecord.SPECIMEN_SOURCE_CAPILLARY_BLOOD -> BloodGlucoseDefinition(
            profile = HealthConnectContract.HEALTH_CONNECT_CAPILLARY_BLOOD_GLUCOSE_PROFILE,
            loinc = "32016-8",
            loincDisplay = "Glucose [Mass/volume] in Capillary blood",
            specimenSourceCode = "SPECIMEN_SOURCE_CAPILLARY_BLOOD",
            specimenType = Coding(
                HealthConnectContract.SNOMED_CT,
                "122554006",
                "Capillary blood specimen",
            ),
        )
        BloodGlucoseRecord.SPECIMEN_SOURCE_PLASMA -> BloodGlucoseDefinition(
            profile = HealthConnectContract.HEALTH_CONNECT_SERUM_PLASMA_GLUCOSE_PROFILE,
            loinc = "2345-7",
            loincDisplay = "Glucose [Mass/volume] in Serum or Plasma",
            specimenSourceCode = "SPECIMEN_SOURCE_PLASMA",
            specimenType = Coding(
                HealthConnectContract.SNOMED_CT,
                "119361006",
                "Plasma specimen",
            ),
        )
        BloodGlucoseRecord.SPECIMEN_SOURCE_SERUM -> BloodGlucoseDefinition(
            profile = HealthConnectContract.HEALTH_CONNECT_SERUM_PLASMA_GLUCOSE_PROFILE,
            loinc = "2345-7",
            loincDisplay = "Glucose [Mass/volume] in Serum or Plasma",
            specimenSourceCode = "SPECIMEN_SOURCE_SERUM",
            specimenType = Coding(
                HealthConnectContract.SNOMED_CT,
                "119364003",
                "Serum specimen",
            ),
        )
        BloodGlucoseRecord.SPECIMEN_SOURCE_INTERSTITIAL_FLUID -> BloodGlucoseDefinition(
            profile = HealthConnectContract.HEALTH_CONNECT_INTERSTITIAL_GLUCOSE_PROFILE,
            loinc = "99504-3",
            loincDisplay = "Glucose [Mass/volume] in Interstitial fluid",
            specimenSourceCode = "SPECIMEN_SOURCE_INTERSTITIAL_FLUID",
            specimenType = Coding(
                HealthConnectContract.SNOMED_CT,
                "258479004",
                "Interstitial fluid specimen",
            ),
        )
        BloodGlucoseRecord.SPECIMEN_SOURCE_TEARS -> throw InvalidHealthConnectRecord(
            "Health Connect tear glucose has no admitted shared Grove Mobile profile in 0.3.0.",
        )
        BloodGlucoseRecord.SPECIMEN_SOURCE_UNKNOWN -> throw InvalidHealthConnectRecord(
            "Health Connect blood glucose requires an explicit supported specimen source.",
        )
        else -> throw InvalidHealthConnectRecord(
            "Unsupported Health Connect blood-glucose specimen source: $specimenSource",
        )
    }

    private fun specimenIdentifier(sourceIdentifier: Identifier, specimenSourceCode: String): Identifier =
        HealthConnectIdentity.specimen(sourceIdentifier, specimenSourceCode)

    private fun glucoseMealContext(record: BloodGlucoseRecord): Extension? {
        val relation = bloodGlucoseRelationToMeal(record.relationToMeal)
        val meal = bloodGlucoseMealType(record.mealType)
        if (relation == null && meal == null) return null
        return Extension(HealthConnectContract.HEALTH_CONNECT_GLUCOSE_MEAL_CONTEXT).apply {
            relation?.let { addExtension(Extension("relationToMeal", it)) }
            meal?.let { addExtension(Extension("mealType", it)) }
        }
    }

    private fun recordingMethod(metadata: Metadata): Extension? {
        val code = when (metadata.recordingMethod) {
            Metadata.RECORDING_METHOD_UNKNOWN -> return null
            Metadata.RECORDING_METHOD_ACTIVELY_RECORDED -> "actively-recorded"
            Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED -> "automatically-recorded"
            Metadata.RECORDING_METHOD_MANUAL_ENTRY -> "manual-entry"
            else -> throw InvalidHealthConnectRecord("Unsupported Health Connect recording method: ${metadata.recordingMethod}")
        }
        return Extension(
            HealthConnectContract.RECORDING_METHOD_EXTENSION,
            Coding(HealthConnectContract.GROVE_RECORDING_METHOD, code, null),
        )
    }

    private fun quantity(value: BigDecimal, code: String, unit: String): Quantity =
        Quantity().setValue(value).setSystem(HealthConnectContract.UCUM).setCode(code).setUnit(unit)

    private fun Instant.fhirDateTime(zoneOffset: ZoneOffset?, field: String): String {
        HealthConnectWireFormat.requireFhirInstant(this, field)
        val canonical = mobileEffectiveInstant()
        HealthConnectWireFormat.requireFhirInstant(
            canonical,
            "$field after Mobile millisecond canonicalization",
        )
        if (zoneOffset == null) return canonical.toString()
        if (
            zoneOffset.totalSeconds % SECONDS_PER_MINUTE != 0 ||
            zoneOffset.totalSeconds !in -MAX_FHIR_OFFSET_SECONDS..MAX_FHIR_OFFSET_SECONDS
        ) {
            throw InvalidHealthConnectRecord(
                "$field offset must use whole minutes in the FHIR range -14:00 through +14:00.",
            )
        }
        val local = canonical.atOffset(zoneOffset)
        if (local.year !in MIN_FHIR_YEAR..MAX_FHIR_YEAR) {
            throw InvalidHealthConnectRecord("$field must retain a four-digit FHIR year after applying its offset.")
        }
        return FHIR_OFFSET_DATE_TIME.format(local)
    }

    /** Applies the source-neutral Mobile effective-time policy without altering identity instants. */
    private fun Instant.mobileEffectiveInstant(): Instant {
        val exactEpochMilliseconds = BigDecimal.valueOf(epochSecond)
            .multiply(MILLISECONDS_PER_SECOND)
            .add(BigDecimal.valueOf(nano.toLong(), NANOSECONDS_TO_MILLISECONDS_SCALE))
        return Instant.ofEpochMilli(
            exactEpochMilliseconds.setScale(0, RoundingMode.HALF_EVEN).longValueExact(),
        )
    }

    private companion object {
        val FHIR_OFFSET_DATE_TIME: DateTimeFormatter = DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd'T'HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .appendOffsetId()
            .toFormatter()

        const val ACTIVE_CALORIES_BURNED_RECORD = "ActiveCaloriesBurnedRecord"
        const val BASAL_BODY_TEMPERATURE_RECORD = "BasalBodyTemperatureRecord"
        const val BASAL_METABOLIC_RATE_RECORD = "BasalMetabolicRateRecord"
        const val BLOOD_GLUCOSE_RECORD = "BloodGlucoseRecord"
        const val BLOOD_PRESSURE_RECORD = "BloodPressureRecord"
        const val BODY_FAT_RECORD = "BodyFatRecord"
        const val BODY_TEMPERATURE_RECORD = "BodyTemperatureRecord"
        const val BODY_WATER_MASS_RECORD = "BodyWaterMassRecord"
        const val BONE_MASS_RECORD = "BoneMassRecord"
        const val CERVICAL_MUCUS_RECORD = "CervicalMucusRecord"
        const val CYCLING_PEDALING_CADENCE_RECORD = "CyclingPedalingCadenceRecord"
        const val DISTANCE_RECORD = "DistanceRecord"
        const val ELEVATION_GAINED_RECORD = "ElevationGainedRecord"
        const val EXERCISE_SESSION_RECORD = "ExerciseSessionRecord"
        const val FLOORS_CLIMBED_RECORD = "FloorsClimbedRecord"
        const val HEART_RATE_RECORD = "HeartRateRecord"
        const val HEART_RATE_VARIABILITY_RMSSD_RECORD = "HeartRateVariabilityRmssdRecord"
        const val HEIGHT_RECORD = "HeightRecord"
        const val HYDRATION_RECORD = "HydrationRecord"
        const val INTERMENSTRUAL_BLEEDING_RECORD = "IntermenstrualBleedingRecord"
        const val LEAN_BODY_MASS_RECORD = "LeanBodyMassRecord"
        const val MENSTRUATION_FLOW_RECORD = "MenstruationFlowRecord"
        const val MENSTRUATION_PERIOD_RECORD = "MenstruationPeriodRecord"
        const val MINDFULNESS_SESSION_RECORD = "MindfulnessSessionRecord"
        const val NUTRITION_RECORD = "NutritionRecord"
        const val OVULATION_TEST_RECORD = "OvulationTestRecord"
        const val OXYGEN_SATURATION_RECORD = "OxygenSaturationRecord"
        const val POWER_RECORD = "PowerRecord"
        const val RESPIRATORY_RATE_RECORD = "RespiratoryRateRecord"
        const val RESTING_HEART_RATE_RECORD = "RestingHeartRateRecord"
        const val SEXUAL_ACTIVITY_RECORD = "SexualActivityRecord"
        const val SKIN_TEMPERATURE_RECORD = "SkinTemperatureRecord"
        const val SLEEP_SESSION_RECORD = "SleepSessionRecord"
        const val SPEED_RECORD = "SpeedRecord"
        const val STEPS_CADENCE_RECORD = "StepsCadenceRecord"
        const val MAX_STEP_COUNT = 1_000_000L
        const val MAX_WHEELCHAIR_PUSH_COUNT = 1_000_000L
        const val MAX_WEIGHT_KILOGRAMS = 1_000.0
        const val STEPS_RECORD = "StepsRecord"
        const val TOTAL_CALORIES_BURNED_RECORD = "TotalCaloriesBurnedRecord"
        const val VO2_MAX_RECORD = "Vo2MaxRecord"
        const val WEIGHT_RECORD = "WeightRecord"
        const val WHEELCHAIR_PUSHES_RECORD = "WheelchairPushesRecord"

        const val ACTIVITY_CATEGORY = "activity"
        const val LABORATORY_CATEGORY = "laboratory"
        const val VITAL_SIGNS_CATEGORY = "vital-signs"

        const val CENTIMETERS_PER_METER = 100.0
        val NANOSECONDS_PER_HOUR: BigDecimal = BigDecimal("3600000000000")
        val NANOSECONDS_PER_MINUTE: BigDecimal = BigDecimal("60000000000")
        val MILLISECONDS_PER_SECOND: BigDecimal = BigDecimal("1000")
        const val SESSION_DURATION_SCALE = 12
        const val NANOSECONDS_TO_MILLISECONDS_SCALE = 6

        val PREVIEW_EVENT_SEQUENCE = EventSequence("1")

        const val SECONDS_PER_MINUTE = 60
        const val MAX_FHIR_OFFSET_SECONDS = 14 * 60 * SECONDS_PER_MINUTE
        const val MIN_FHIR_YEAR = 1
        const val MAX_FHIR_YEAR = 9999
    }
}

private fun outputIdentifier(observation: Observation): Identifier = observationIdentity(observation)

internal fun concept(system: String, code: String, display: String): CodeableConcept =
    CodeableConcept(Coding(system, code, display))
