//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health

/**
 * The code and unit an observation of a given sample type carries.
 *
 * Every value here is taken from the backend's own tables — `functions/src/models/codes/codes.ts`
 * for the LOINC codes and `quantityUnit.ts` for the UCUM units — rather than chosen here, because
 * the backend's converters match on them and an observation carrying anything else is dropped on
 * ingestion.
 *
 * @param code The code identifying what was measured.
 * @param display A human-readable name for [code].
 * @param unit The UCUM code the value is expressed in.
 * @param unitDisplay A human-readable name for [unit].
 * @param system The code system [code] belongs to.
 */
data class HealthObservationCoding(
    val code: String,
    val display: String,
    val unit: String?,
    val unitDisplay: String?,
    val system: String = LOINC_SYSTEM,
) {

    companion object {

        /**
         * The LOINC code system, matching the backend's `CodingSystem.loinc`.
         */
        const val LOINC_SYSTEM = "http://loinc.org"

        /**
         * The code system for a measurement Health Connect offers that LOINC and the backend's own
         * table have no entry for. Namespaced to the study so it cannot collide with a real LOINC
         * code, and carrying the Health Connect record type as its code.
         */
        const val HEALTH_CONNECT_SYSTEM = "https://myheartcounts.stanford.edu/fhir/CodeSystem/health-connect"

        /**
         * The UCUM system every unit below is expressed in.
         */
        const val UCUM_SYSTEM = "http://unitsofmeasure.org"

        /**
         * The coding for [sampleTypeIdentifier], or `null` when the backend defines none — in which
         * case [forHealthConnectRecord] supplies an Android-native one instead.
         */
        fun forSampleType(sampleTypeIdentifier: String): HealthObservationCoding? =
            BY_SAMPLE_TYPE[sampleTypeIdentifier]

        /**
         * A coding for a Health Connect record type that has no shared counterpart, naming the
         * record type itself so the measurement stays identifiable downstream.
         *
         * @param recordTypeIdentifier A Health Connect record type identifier, such as `SpeedRecord`.
         * @param unit The UCUM code the value is expressed in, or `null` for a non-numeric record.
         */
        fun forHealthConnectRecord(
            recordTypeIdentifier: String,
            unit: String? = ANDROID_ONLY_UNITS[recordTypeIdentifier],
            unitDisplay: String? = unit,
        ): HealthObservationCoding = HealthObservationCoding(
            code = recordTypeIdentifier,
            display = recordTypeIdentifier.removeSuffix("Record"),
            unit = unit,
            unitDisplay = unitDisplay,
            system = HEALTH_CONNECT_SYSTEM,
        )

        /**
         * UCUM units for the record types Health Connect offers that no shared collection covers.
         *
         * Without an entry a measurement is written as a bare code with no value at all, so every
         * type [HealthSampleType] routes as Android-only needs one here.
         */
        private val ANDROID_ONLY_UNITS: Map<String, String> = mapOf(
            "BasalMetabolicRateRecord" to "kcal/d",
            "HeartRateVariabilityRmssdRecord" to "ms",
            "SpeedRecord" to "m/s",
            "TotalCaloriesBurnedRecord" to "kcal",
        )

        private val BY_SAMPLE_TYPE: Map<String, HealthObservationCoding> = mapOf(
            "HKQuantityTypeIdentifierStepCount" to HealthObservationCoding(
                code = "41950-7",
                display = "Number of steps in unspecified time Pedometer",
                unit = "steps",
                unitDisplay = "steps",
            ),
            "HKQuantityTypeIdentifierHeartRate" to HealthObservationCoding(
                code = "8867-4",
                display = "Heart rate",
                unit = "/min",
                unitDisplay = "beats/minute",
            ),
            "HKQuantityTypeIdentifierRestingHeartRate" to HealthObservationCoding(
                code = "40443-4",
                display = "Heart rate - resting",
                unit = "/min",
                unitDisplay = "beats/minute",
            ),
            "HKQuantityTypeIdentifierDistanceWalkingRunning" to HealthObservationCoding(
                code = "41951-5",
                display = "Distance walked",
                unit = "m",
                unitDisplay = "meters",
            ),
            "HKQuantityTypeIdentifierFlightsClimbed" to HealthObservationCoding(
                code = "93825-7",
                display = "Flights climbed",
                unit = "flights",
                unitDisplay = "flights",
            ),
            "HKQuantityTypeIdentifierOxygenSaturation" to HealthObservationCoding(
                code = "2708-6",
                display = "Oxygen saturation in Arterial blood",
                unit = "%",
                unitDisplay = "percent",
            ),
            "HKQuantityTypeIdentifierRespiratoryRate" to HealthObservationCoding(
                code = "9279-1",
                display = "Respiratory rate",
                unit = "{Breaths}/min",
                unitDisplay = "breaths/minute",
            ),
            "HKQuantityTypeIdentifierBodyTemperature" to HealthObservationCoding(
                code = "8310-5",
                display = "Body temperature",
                unit = "Cel",
                unitDisplay = "celsius",
            ),
            "HKQuantityTypeIdentifierBasalBodyTemperature" to HealthObservationCoding(
                code = "8334-5",
                display = "Body temperature at rest",
                unit = "Cel",
                unitDisplay = "celsius",
            ),
            "HKQuantityTypeIdentifierBloodGlucose" to HealthObservationCoding(
                code = "2339-0",
                display = "Glucose [Mass/volume] in Blood",
                unit = "mg/dL",
                unitDisplay = "mg/dL",
            ),
            "HKQuantityTypeIdentifierBodyMass" to HealthObservationCoding(
                code = "29463-7",
                display = "Body weight",
                unit = "kg",
                unitDisplay = "kg",
            ),
            "HKQuantityTypeIdentifierHeight" to HealthObservationCoding(
                code = "8302-2",
                display = "Body height",
                unit = "cm",
                unitDisplay = "centimeters",
            ),
            "HKQuantityTypeIdentifierBodyFatPercentage" to HealthObservationCoding(
                code = "41982-0",
                display = "Percentage of body fat Measured",
                unit = "%",
                unitDisplay = "percent",
            ),
            "HKQuantityTypeIdentifierLeanBodyMass" to HealthObservationCoding(
                code = "91557-9",
                display = "Lean body mass",
                unit = "kg",
                unitDisplay = "kg",
            ),
            "HKQuantityTypeIdentifierVO2Max" to HealthObservationCoding(
                code = "93837-2",
                display = "VO2 max",
                unit = "mL/kg/min",
                unitDisplay = "mL/kg/min",
            ),
            "HKQuantityTypeIdentifierPushCount" to HealthObservationCoding(
                code = "93824-0",
                display = "Wheelchair pushes",
                unit = "count",
                unitDisplay = "count",
            ),
            "HKCategoryTypeIdentifierMindfulSession" to HealthObservationCoding(
                code = "93844-8",
                display = "Mindful session",
                unit = "min",
                unitDisplay = "minutes",
            ),
            "HKCategoryTypeIdentifierSleepAnalysis" to HealthObservationCoding(
                code = "93832-3",
                display = "Sleep analysis",
                unit = "min",
                unitDisplay = "minutes",
            ),
            "HKWorkoutTypeIdentifier" to HealthObservationCoding(
                code = "93849-7",
                display = "Workout",
                unit = "min",
                unitDisplay = "minutes",
            ),
            // Blood pressure carries its two readings as components rather than a single value, so
            // the top-level coding has no unit of its own.
            "HKCorrelationTypeIdentifierBloodPressure" to HealthObservationCoding(
                code = "85354-9",
                display = "Blood pressure panel with all children optional",
                unit = null,
                unitDisplay = null,
            ),
            // The two component codings of a blood pressure observation.
            SYSTOLIC_COMPONENT to HealthObservationCoding(
                code = "8480-6",
                display = "Systolic blood pressure",
                unit = "mm[Hg]",
                unitDisplay = "mmHg",
            ),
            DIASTOLIC_COMPONENT to HealthObservationCoding(
                code = "8462-4",
                display = "Diastolic blood pressure",
                unit = "mm[Hg]",
                unitDisplay = "mmHg",
            ),
            "HKQuantityTypeIdentifierActiveEnergyBurned" to HealthObservationCoding(
                code = "41981-2",
                display = "Calories burned",
                unit = "kcal",
                unitDisplay = "kcal",
            ),
        )
    }
}

/**
 * Key for the systolic reading's coding; not a sample type identifier of its own.
 */
const val SYSTOLIC_COMPONENT = "systolicBloodPressure"

/**
 * Key for the diastolic reading's coding; not a sample type identifier of its own.
 */
const val DIASTOLIC_COMPONENT = "diastolicBloodPressure"
