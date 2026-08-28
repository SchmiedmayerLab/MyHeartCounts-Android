//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

/** What this producer deliberately does with one public AndroidX Health Connect source field. */
internal data class HealthConnectFieldDisposition(
    val status: Status,
    val fhirPath: String? = null,
    val rationale: String? = null,
) {
    enum class Status { MAPPED, INTENTIONALLY_OMITTED, REJECTED, UNAVAILABLE }

    init {
        require((status == Status.MAPPED) == (fhirPath != null)) {
            "A mapped source field requires one FHIR destination; other dispositions must not claim one."
        }
        require(status == Status.MAPPED || !rationale.isNullOrBlank()) {
            "A non-mapped source field requires a reviewed rationale."
        }
    }
}

/**
 * Executable AndroidX 1.1.0 field inventory for every source type this producer calls supported.
 *
 * Tests compare these keys to the pinned bytecode's public getters, so an AndroidX field addition
 * cannot silently inherit "supported" status. Nested source types are inventoried separately.
 */
@Suppress("LargeClass")
internal object HealthConnectFieldDispositions {
    const val SOURCE_VERSION = "1.1.0"

    private fun mapped(path: String) = HealthConnectFieldDisposition(
        HealthConnectFieldDisposition.Status.MAPPED,
        fhirPath = path,
    )

    private fun omitted(reason: String) = HealthConnectFieldDisposition(
        HealthConnectFieldDisposition.Status.INTENTIONALLY_OMITTED,
        rationale = reason,
    )

    private val instant = mapOf(
        "time" to mapped("Observation.effectiveDateTime"),
        "zoneOffset" to mapped("Observation.effectiveDateTime lexical offset"),
        "metadata" to mapped("Observation.identifier/issued/device/extension and Provenance.entity/agent"),
    )
    private val interval = mapOf(
        "startTime" to mapped("Observation.effectivePeriod.start"),
        "startZoneOffset" to mapped("Observation.effectivePeriod.start lexical offset"),
        "endTime" to mapped("Observation.effectivePeriod.end"),
        "endZoneOffset" to mapped("Observation.effectivePeriod.end lexical offset"),
        "metadata" to mapped("Observation.identifier/issued/device/extension and Provenance.entity/agent"),
    )
    private fun instant(vararg fields: Pair<String, String>) =
        instant + fields.associate { (field, path) -> field to mapped(path) }
    private fun interval(vararg fields: Pair<String, String>) =
        interval + fields.associate { (field, path) -> field to mapped(path) }

    private val nutrientFields = setOf(
        "biotin", "caffeine", "calcium", "energy", "energyFromFat", "chloride", "cholesterol", "chromium",
        "copper", "dietaryFiber", "folate", "folicAcid", "iodine", "iron", "magnesium", "manganese",
        "molybdenum", "monounsaturatedFat", "niacin", "pantothenicAcid", "phosphorus", "polyunsaturatedFat",
        "potassium", "protein", "riboflavin", "saturatedFat", "selenium", "sodium", "sugar", "thiamin",
        "totalCarbohydrate", "totalFat", "transFat", "unsaturatedFat", "vitaminA", "vitaminB12", "vitaminB6",
        "vitaminC", "vitaminD", "vitaminE", "vitaminK", "zinc",
    )

    val records: Map<String, Map<String, HealthConnectFieldDisposition>> = mapOf(
        "ActiveCaloriesBurnedRecord" to interval("energy" to "Observation.valueQuantity"),
        "BasalBodyTemperatureRecord" to instant(
            "temperature" to "Observation.valueQuantity",
            "measurementLocation" to "Observation.bodySite",
        ),
        "BasalMetabolicRateRecord" to instant("basalMetabolicRate" to "Observation.valueQuantity"),
        "BloodGlucoseRecord" to instant(
            "level" to "Observation.valueQuantity",
            "specimenSource" to "Observation.specimen and specimen-specific profile/code",
            "mealType" to "health-connect-glucose-meal-context extension",
            "relationToMeal" to "health-connect-glucose-meal-context extension",
        ),
        "BloodPressureRecord" to instant(
            "systolic" to "Observation.component[systolic].valueQuantity",
            "diastolic" to "Observation.component[diastolic].valueQuantity",
            "bodyPosition" to "observation-bodyPosition extension",
            "measurementLocation" to "Observation.bodySite",
        ),
        "BodyFatRecord" to instant("percentage" to "Observation.valueQuantity"),
        "BodyTemperatureRecord" to instant(
            "temperature" to "Observation.valueQuantity",
            "measurementLocation" to "Observation.bodySite",
        ),
        "BodyWaterMassRecord" to instant("mass" to "Observation.valueQuantity"),
        "BoneMassRecord" to instant("mass" to "Observation.valueQuantity"),
        "CervicalMucusRecord" to instant(
            "appearance" to "Observation.valueCodeableConcept",
            "sensation" to "Observation.component[cervical-mucus-sensation]",
        ),
        "CyclingPedalingCadenceRecord" to interval("samples" to "one Observation per Sample"),
        "DistanceRecord" to interval("distance" to "Observation.valueQuantity"),
        "ElevationGainedRecord" to interval("elevation" to "Observation.valueQuantity"),
        "ExerciseSessionRecord" to interval(
            "exerciseType" to "Observation.valueCodeableConcept",
            "title" to "health-connect-session-title extension when retention policy admits it",
            "notes" to "Observation.note.text when retention policy admits it",
            "segments" to "one workout-segment member Observation per ExerciseSegment",
            "laps" to "one workout-segment member Observation per ExerciseLap",
        ) + mapOf(
            "exerciseRouteResult" to omitted(
                "Grove 0.6.0 admits no safe route geometry profile; route data requires a separately reviewed source artifact.",
            ),
            "plannedExerciseSessionId" to omitted(
                "The referenced PlannedExerciseSessionRecord source type is explicitly deferred in AndroidX 1.1.0 support.",
            ),
        ),
        "FloorsClimbedRecord" to interval("floors" to "Observation.valueQuantity"),
        "HeartRateRecord" to interval("samples" to "one Observation per Sample"),
        "HeartRateVariabilityRmssdRecord" to instant(
            "heartRateVariabilityMillis" to "Observation.valueQuantity",
        ),
        "HeightRecord" to instant("height" to "Observation.valueQuantity"),
        "HydrationRecord" to interval("volume" to "Observation.valueQuantity"),
        "IntermenstrualBleedingRecord" to instant(),
        "LeanBodyMassRecord" to instant("mass" to "Observation.valueQuantity"),
        "MenstruationFlowRecord" to instant("flow" to "Observation.valueCodeableConcept"),
        "MenstruationPeriodRecord" to interval(),
        "MindfulnessSessionRecord" to interval(
            "mindfulnessSessionType" to "Observation.method",
            "title" to "health-connect-session-title extension when retention policy admits it",
            "notes" to "Observation.note.text when retention policy admits it",
        ),
        "NutritionRecord" to nutrition(),
        "OvulationTestRecord" to instant("result" to "Observation.valueCodeableConcept"),
        "OxygenSaturationRecord" to instant("percentage" to "Observation.valueQuantity"),
        "PowerRecord" to interval("samples" to "one Observation per Sample"),
        "RespiratoryRateRecord" to instant("rate" to "Observation.valueQuantity"),
        "RestingHeartRateRecord" to instant("beatsPerMinute" to "Observation.valueQuantity"),
        "SexualActivityRecord" to instant("protectionUsed" to "Observation.valueCodeableConcept"),
        "SkinTemperatureRecord" to interval(
            "deltas" to "one Observation per Delta",
            "baseline" to "combined with Delta for Observation.valueQuantity",
            "measurementLocation" to "Observation.bodySite",
        ),
        "SleepSessionRecord" to interval(
            "title" to "health-connect-session-title extension when retention policy admits it",
            "notes" to "Observation.note.text when retention policy admits it",
            "stages" to "one member Observation per Stage in exact platform-list occurrence order",
        ),
        "SpeedRecord" to interval("samples" to "one Observation per Sample"),
        "StepsCadenceRecord" to interval("samples" to "one Observation per Sample"),
        "StepsRecord" to interval("count" to "Observation.valueQuantity"),
        "TotalCaloriesBurnedRecord" to interval("energy" to "Observation.valueQuantity"),
        "Vo2MaxRecord" to instant(
            "vo2MillilitersPerMinuteKilogram" to "Observation.valueQuantity",
            "measurementMethod" to "Observation.method",
        ),
        "WeightRecord" to instant("weight" to "Observation.valueQuantity"),
        "WheelchairPushesRecord" to interval("count" to "Observation.valueQuantity"),
    )

    val metadata: Map<String, HealthConnectFieldDisposition> = mapOf(
        "recordingMethod" to mapped("grove-recording-method extension"),
        "id" to mapped(
            "opaque source-record/source-output HMAC input; optional governed Identifier on the designated primary output",
        ),
        "dataOrigin" to mapped("source-application Provenance agent and writer-application identity"),
        "lastModifiedTime" to mapped("Observation.issued and durable source-version state"),
        "clientRecordId" to mapped("opaque writer-record identifier"),
        "clientRecordVersion" to mapped("grove-writer-record-version extension"),
        "device" to mapped("explicitly governed recording Device reference when a stable per-unit token exists"),
    )

    val nested: Map<String, Map<String, HealthConnectFieldDisposition>> = mapOf(
        "HeartRateRecord.Sample" to mapOf(
            "time" to mapped("Observation.effectiveDateTime and source-list occurrence output discriminator"),
            "beatsPerMinute" to mapped("Observation.valueQuantity"),
        ),
        "CyclingPedalingCadenceRecord.Sample" to mapOf(
            "time" to mapped("Observation.effectiveDateTime and source-list occurrence output discriminator"),
            "revolutionsPerMinute" to mapped("Observation.valueQuantity"),
        ),
        "PowerRecord.Sample" to mapOf(
            "time" to mapped("Observation.effectiveDateTime and source-list occurrence output discriminator"),
            "power" to mapped("Observation.valueQuantity"),
        ),
        "SpeedRecord.Sample" to mapOf(
            "time" to mapped("Observation.effectiveDateTime and source-list occurrence output discriminator"),
            "speed" to mapped("Observation.valueQuantity"),
        ),
        "StepsCadenceRecord.Sample" to mapOf(
            "time" to mapped("Observation.effectiveDateTime and source-list occurrence output discriminator"),
            "rate" to mapped("Observation.valueQuantity"),
        ),
        "SleepSessionRecord.Stage" to mapOf(
            "startTime" to mapped("member Observation.effectivePeriod.start and occurrence discriminator"),
            "endTime" to mapped("member Observation.effectivePeriod.end and occurrence discriminator"),
            "stage" to mapped("member Observation.valueCodeableConcept and occurrence discriminator"),
        ),
        "SkinTemperatureRecord.Delta" to mapOf(
            "time" to mapped("Observation.effectiveDateTime and source-list occurrence output discriminator"),
            "delta" to mapped("combined with baseline for Observation.valueQuantity"),
        ),
        "ExerciseSegment" to mapOf(
            "startTime" to mapped("member Observation.effectivePeriod.start"),
            "endTime" to mapped("member Observation.effectivePeriod.end"),
            "segmentType" to mapped("member Observation.valueCodeableConcept"),
            "repetitions" to mapped("member Observation.component[repetitions]"),
        ),
        "ExerciseLap" to mapOf(
            "startTime" to mapped("member Observation.effectivePeriod.start"),
            "endTime" to mapped("member Observation.effectivePeriod.end"),
            "length" to mapped("member Observation.component[lap-length]"),
        ),
    )

    init {
        check(records.keys == HealthConnectCatalog.supportedRecordTypeIdentifiers) {
            "Every supported Health Connect type requires one field-disposition inventory."
        }
    }

    private fun nutrition(): Map<String, HealthConnectFieldDisposition> = interval +
        nutrientFields.associateWith { mapped("one nutrient Observation.valueQuantity when present") } +
        mapOf(
            "name" to omitted(
                "Free-text meal names are not retained without a dedicated deployment privacy policy and profile element.",
            ),
            "mealType" to omitted(
                "The current nutrition profiles describe nutrient results and do not define meal-event context semantics.",
            ),
        )
}
