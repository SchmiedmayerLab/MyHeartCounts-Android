//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import org.grovealliance.health.RecordType

/** Closed source-type inventory admitted by the current Health Connect FHIR producer. */
internal object HealthConnectCatalog {
    /** Exact AndroidX 1.1.0 inventory surfaced by this version of Grove Health. */
    val allRecordTypeIdentifiers: Set<String> = RecordType.all.mapTo(mutableSetOf()) { it.identifier }

    val supportedRecordTypeIdentifiers: Set<String> = setOf(
        "ActiveCaloriesBurnedRecord",
        "BasalBodyTemperatureRecord",
        "BasalMetabolicRateRecord",
        "BloodGlucoseRecord",
        "BloodPressureRecord",
        "BodyFatRecord",
        "BodyTemperatureRecord",
        "BodyWaterMassRecord",
        "BoneMassRecord",
        "CervicalMucusRecord",
        "CyclingPedalingCadenceRecord",
        "DistanceRecord",
        "ElevationGainedRecord",
        "ExerciseSessionRecord",
        "FloorsClimbedRecord",
        "HeartRateRecord",
        "HeartRateVariabilityRmssdRecord",
        "HeightRecord",
        "HydrationRecord",
        "IntermenstrualBleedingRecord",
        "LeanBodyMassRecord",
        "MenstruationFlowRecord",
        "MenstruationPeriodRecord",
        "MindfulnessSessionRecord",
        "NutritionRecord",
        "OvulationTestRecord",
        "OxygenSaturationRecord",
        "PowerRecord",
        "RespiratoryRateRecord",
        "RestingHeartRateRecord",
        "SexualActivityRecord",
        "SkinTemperatureRecord",
        "SleepSessionRecord",
        "SpeedRecord",
        "StepsCadenceRecord",
        "StepsRecord",
        "TotalCaloriesBurnedRecord",
        "Vo2MaxRecord",
        "WeightRecord",
        "WheelchairPushesRecord",
    )

    /** Exact source inventory that is deliberately not emitted yet. */
    val deferredRecordTypeIdentifiers: Set<String> = setOf(
        "PlannedExerciseSessionRecord",
    )

    /** Supported source types whose successful conversion may legitimately produce no outputs. */
    val zeroOutputRecordTypeIdentifiers: Set<String> = setOf(
        "CyclingPedalingCadenceRecord",
        "HeartRateRecord",
        "NutritionRecord",
        "PowerRecord",
        "SkinTemperatureRecord",
        "SpeedRecord",
        "StepsCadenceRecord",
    )

    init {
        check(supportedRecordTypeIdentifiers.intersect(deferredRecordTypeIdentifiers).isEmpty()) {
            "A Health Connect source type cannot be both supported and deferred."
        }
        check(supportedRecordTypeIdentifiers + deferredRecordTypeIdentifiers == allRecordTypeIdentifiers) {
            "Every AndroidX Health Connect 1.1 source type must have one explicit adapter status."
        }
        check(zeroOutputRecordTypeIdentifiers.all { it in supportedRecordTypeIdentifiers }) {
            "Only a supported Health Connect source type may claim zero-output conversions."
        }
    }
}
