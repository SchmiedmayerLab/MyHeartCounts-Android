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
import org.grovealliance.health.RecordType
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.StringType
import java.math.BigDecimal
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

/** Converts the explicitly supported Health Connect records into the Grove Mobile R4 contract. */
@OptIn(ExperimentalMindfulnessSessionApi::class)
@Suppress("CyclomaticComplexMethod")
class HealthConnectConverter(
    internal val context: HealthConnectConversionContext,
    internal val synchronizationScope: HealthConnectSynchronizationScope,
) {
    internal val entryNodeIdentifierSystem: String
        get() = context.entryNodeIdentifierSystem

    internal fun convert(record: Record, convertedAt: Instant, eventSequence: EventSequence): HealthConnectConversion {
        return convertWithEventToken(record, convertedAt, eventSequence)
    }

    /** Converts record-data failures into a stable typed result for collector telemetry and flow control. */
    fun convertOutcome(
        record: Record,
        convertedAt: Instant,
        eventSequence: EventSequence,
    ): HealthConnectConversionOutcome = try {
        HealthConnectConversionOutcome.Converted(convertWithEventToken(record, convertedAt, eventSequence))
    } catch (error: UnsupportedHealthConnectRecord) {
        HealthConnectConversionOutcome.Unsupported(error.recordType, requireNotNull(error.message))
    } catch (error: InvalidHealthConnectRecord) {
        HealthConnectConversionOutcome.Rejected(requireNotNull(error.message))
    }

    internal fun preview(
        record: Record,
        convertedAt: Instant,
        priorEventSequence: EventSequence?,
    ): HealthConnectConversion =
        // Event-time Device snapshots do depend on the event. Reusing the prior sequence makes the
        // comparison graph address the prior snapshots, while a new record uses a valid placeholder.
        convertWithEventToken(record, convertedAt, priorEventSequence ?: PREVIEW_EVENT_SEQUENCE)

    internal fun bundleIdentifier(eventSequence: EventSequence): Identifier =
        HealthConnectIdentity.exchange(
            context.eventIdentifierSystem,
            synchronizationScope.producerInstance,
            eventSequence,
        )

    internal fun assemblerSnapshotIdentifier(eventSequence: EventSequence): Identifier =
        context.assemblerSnapshotIdentifier(
            synchronizationScope.identityKey,
            bundleIdentifier(eventSequence),
        )

    internal fun convertWithEventToken(
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
            else -> throw UnsupportedHealthConnectRecord(RecordType.from(record).identifier)
        }
    }

    internal fun Observation.retainSessionText(
        title: String?,
        notes: String?,
        sourceType: String,
    ) {
        if (context.userAuthoredTextPolicy != HealthConnectUserAuthoredTextPolicy.RETAIN) return
        title?.takeIf(String::isNotBlank)?.let {
            addExtension(
                Extension(
                    HealthConnectContract.HEALTH_CONNECT_SESSION_TITLE,
                    StringType(requireSourceScalarText(it, "$sourceType.title")),
                ),
            )
        }
        notes?.takeIf(String::isNotBlank)?.let {
            addNote().text = requireSourceScalarText(it, "$sourceType.notes")
        }
    }

    /** Carries the writer's own identity for the record, when it assigns one. */
    internal fun Observation.clientRecordIdentity(metadata: Metadata) {
        // A writer that re-imports a measurement reuses its clientRecordId and raises the version,
        // and the stored Record then carries a new metadata.id. Without this the same measurement
        // is counted twice; with it a receiver supersedes the lower version.
        val clientRecordId = metadata.validatedClientRecordId() ?: return
        // A clientRecordId is unique only within the app that wrote it, so the writer is part of
        // the identity. Without it two apps that both chose "weighin-2026-08-19" would look like
        // one measurement, and a receiver applying the supersession rule would drop one of them.
        val writer = metadata.dataOrigin.packageName
        if (writer.isBlank()) {
            throw InvalidHealthConnectRecord("A client record identity requires its writer package name.")
        }
        addIdentifier(
            HealthConnectIdentity.writerRecord(
                synchronizationScope.identityKey,
                FhirIdentifierKey(HealthConnectContract.ANDROID_PACKAGE_IDENTIFIER, writer),
                clientRecordId,
            ),
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

    internal companion object {
        val FHIR_OFFSET_DATE_TIME: DateTimeFormatter = DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd'T'HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .appendOffsetId()
            .toFormatter()
        val EFFECTIVE_DATE_TIME_ORDER = compareBy<DateTimeType>({ it.value.time }, { it.valueAsString })

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

internal fun outputIdentifier(observation: Observation): Identifier = observationIdentity(observation)

internal fun concept(system: String, code: String, display: String): CodeableConcept =
    CodeableConcept(Coding(system, code, display))
