//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyTemperatureMeasurementLocation
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding

/** Maps each Health Connect measurement-context constant onto its standard coding. */
internal fun sleepStageCoding(stage: Int): SourceCodedValue = when (stage) {
    SleepSessionRecord.STAGE_TYPE_UNKNOWN ->
        SourceCodedValue(
            "unknown",
            "Unknown sleep stage",
            "STAGE_TYPE_UNKNOWN",
            "Unknown",
        )
    SleepSessionRecord.STAGE_TYPE_AWAKE ->
        SourceCodedValue("awake", "Awake", "STAGE_TYPE_AWAKE", "Awake")
    SleepSessionRecord.STAGE_TYPE_SLEEPING ->
        SourceCodedValue(
            "asleep-unspecified",
            "Asleep, unspecified stage",
            "STAGE_TYPE_SLEEPING",
            "Sleeping",
        )
    SleepSessionRecord.STAGE_TYPE_OUT_OF_BED ->
        SourceCodedValue("out-of-bed", "Out of bed", "STAGE_TYPE_OUT_OF_BED", "Out of bed")
    SleepSessionRecord.STAGE_TYPE_LIGHT ->
        SourceCodedValue("light", "Light sleep", "STAGE_TYPE_LIGHT", "Light sleep")
    SleepSessionRecord.STAGE_TYPE_DEEP ->
        SourceCodedValue("deep", "Deep sleep", "STAGE_TYPE_DEEP", "Deep sleep")
    SleepSessionRecord.STAGE_TYPE_REM ->
        SourceCodedValue("rem", "REM sleep", "STAGE_TYPE_REM", "REM sleep")
    SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED ->
        SourceCodedValue("awake", "Awake", "STAGE_TYPE_AWAKE_IN_BED", "Awake in bed")
    else -> throw InvalidHealthConnectRecord("Unsupported Health Connect sleep stage: $stage")
}

@OptIn(ExperimentalMindfulnessSessionApi::class)
internal fun mindfulnessSessionTypeCoding(value: Int): Coding = when (value) {
    MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_UNKNOWN ->
        Coding(
            HealthConnectContract.HEALTH_CONNECT_MINDFULNESS_SESSION_TYPE,
            "MINDFULNESS_SESSION_TYPE_UNKNOWN",
            "Unknown",
        )
    MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION ->
        Coding(
            HealthConnectContract.HEALTH_CONNECT_MINDFULNESS_SESSION_TYPE,
            "MINDFULNESS_SESSION_TYPE_MEDITATION",
            "Meditation",
        )
    MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_BREATHING ->
        Coding(
            HealthConnectContract.HEALTH_CONNECT_MINDFULNESS_SESSION_TYPE,
            "MINDFULNESS_SESSION_TYPE_BREATHING",
            "Guided breathing",
        )
    MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MUSIC ->
        Coding(
            HealthConnectContract.HEALTH_CONNECT_MINDFULNESS_SESSION_TYPE,
            "MINDFULNESS_SESSION_TYPE_MUSIC",
            "Music or soundscapes",
        )
    MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT ->
        Coding(
            HealthConnectContract.HEALTH_CONNECT_MINDFULNESS_SESSION_TYPE,
            "MINDFULNESS_SESSION_TYPE_MOVEMENT",
            "Movement",
        )
    MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_UNGUIDED ->
        Coding(
            HealthConnectContract.HEALTH_CONNECT_MINDFULNESS_SESSION_TYPE,
            "MINDFULNESS_SESSION_TYPE_UNGUIDED",
            "Unguided",
        )
    else -> throw InvalidHealthConnectRecord("Unsupported Health Connect mindfulness-session type: $value")
}

internal fun vo2MaxMeasurementMethodCoding(value: Int): Coding = when (value) {
    Vo2MaxRecord.MEASUREMENT_METHOD_OTHER ->
        Coding(HealthConnectContract.HEALTH_CONNECT_VO2_MAX_MEASUREMENT_METHOD, "MEASUREMENT_METHOD_OTHER", "Other")
    Vo2MaxRecord.MEASUREMENT_METHOD_METABOLIC_CART ->
        Coding(
            HealthConnectContract.HEALTH_CONNECT_VO2_MAX_MEASUREMENT_METHOD,
            "MEASUREMENT_METHOD_METABOLIC_CART",
            "Metabolic cart",
        )
    Vo2MaxRecord.MEASUREMENT_METHOD_HEART_RATE_RATIO ->
        Coding(
            HealthConnectContract.HEALTH_CONNECT_VO2_MAX_MEASUREMENT_METHOD,
            "MEASUREMENT_METHOD_HEART_RATE_RATIO",
            "Heart-rate ratio",
        )
    Vo2MaxRecord.MEASUREMENT_METHOD_COOPER_TEST ->
        Coding(
            HealthConnectContract.HEALTH_CONNECT_VO2_MAX_MEASUREMENT_METHOD,
            "MEASUREMENT_METHOD_COOPER_TEST",
            "Cooper test",
        )
    Vo2MaxRecord.MEASUREMENT_METHOD_MULTISTAGE_FITNESS_TEST ->
        Coding(
            HealthConnectContract.HEALTH_CONNECT_VO2_MAX_MEASUREMENT_METHOD,
            "MEASUREMENT_METHOD_MULTISTAGE_FITNESS_TEST",
            "Multistage fitness test",
        )
    Vo2MaxRecord.MEASUREMENT_METHOD_ROCKPORT_FITNESS_TEST ->
        Coding(
            HealthConnectContract.HEALTH_CONNECT_VO2_MAX_MEASUREMENT_METHOD,
            "MEASUREMENT_METHOD_ROCKPORT_FITNESS_TEST",
            "Rockport fitness test",
        )
    else -> throw InvalidHealthConnectRecord("Unsupported Health Connect VO2 max measurement method: $value")
}

internal fun bloodGlucoseRelationToMeal(value: Int): Coding? = when (value) {
    BloodGlucoseRecord.RELATION_TO_MEAL_UNKNOWN -> null
    BloodGlucoseRecord.RELATION_TO_MEAL_GENERAL -> healthConnectMealCoding(
        HealthConnectContract.HEALTH_CONNECT_RELATION_TO_MEAL,
        "RELATION_TO_MEAL_GENERAL",
        "General",
    )
    BloodGlucoseRecord.RELATION_TO_MEAL_FASTING -> healthConnectMealCoding(
        HealthConnectContract.HEALTH_CONNECT_RELATION_TO_MEAL,
        "RELATION_TO_MEAL_FASTING",
        "Fasting",
    )
    BloodGlucoseRecord.RELATION_TO_MEAL_BEFORE_MEAL -> healthConnectMealCoding(
        HealthConnectContract.HEALTH_CONNECT_RELATION_TO_MEAL,
        "RELATION_TO_MEAL_BEFORE_MEAL",
        "Before meal",
    )
    BloodGlucoseRecord.RELATION_TO_MEAL_AFTER_MEAL -> healthConnectMealCoding(
        HealthConnectContract.HEALTH_CONNECT_RELATION_TO_MEAL,
        "RELATION_TO_MEAL_AFTER_MEAL",
        "After meal",
    )
    else -> throw InvalidHealthConnectRecord("Unsupported Health Connect relation to meal: $value")
}

internal fun bloodGlucoseMealType(value: Int): Coding? = when (value) {
    MealType.MEAL_TYPE_UNKNOWN -> null
    MealType.MEAL_TYPE_BREAKFAST -> healthConnectMealCoding(
        HealthConnectContract.HEALTH_CONNECT_MEAL_TYPE,
        "MEAL_TYPE_BREAKFAST",
        "Breakfast",
    )
    MealType.MEAL_TYPE_LUNCH -> healthConnectMealCoding(
        HealthConnectContract.HEALTH_CONNECT_MEAL_TYPE,
        "MEAL_TYPE_LUNCH",
        "Lunch",
    )
    MealType.MEAL_TYPE_DINNER -> healthConnectMealCoding(
        HealthConnectContract.HEALTH_CONNECT_MEAL_TYPE,
        "MEAL_TYPE_DINNER",
        "Dinner",
    )
    MealType.MEAL_TYPE_SNACK -> healthConnectMealCoding(
        HealthConnectContract.HEALTH_CONNECT_MEAL_TYPE,
        "MEAL_TYPE_SNACK",
        "Snack",
    )
    else -> throw InvalidHealthConnectRecord("Unsupported Health Connect meal type: $value")
}

internal fun healthConnectMealCoding(system: String, code: String, display: String): Coding =
    Coding(system, code, display)

internal fun bloodPressureBodyPosition(value: Int): Coding? = when (value) {
    BloodPressureRecord.BODY_POSITION_UNKNOWN -> null
    BloodPressureRecord.BODY_POSITION_STANDING_UP ->
        Coding(HealthConnectContract.SNOMED_CT, "10904000", "Orthostatic body position")
    BloodPressureRecord.BODY_POSITION_SITTING_DOWN ->
        Coding(HealthConnectContract.SNOMED_CT, "33586001", "Sitting position")
    BloodPressureRecord.BODY_POSITION_LYING_DOWN ->
        Coding(HealthConnectContract.SNOMED_CT, "102538003", "Recumbent body position")
    BloodPressureRecord.BODY_POSITION_RECLINING ->
        Coding(HealthConnectContract.SNOMED_CT, "272580008", "Semi-recumbent position")
    else -> throw InvalidHealthConnectRecord("Unsupported Health Connect blood-pressure body position: $value")
}

internal fun bloodPressureMeasurementLocation(value: Int): CodeableConcept? = when (value) {
    BloodPressureRecord.MEASUREMENT_LOCATION_UNKNOWN -> null
    BloodPressureRecord.MEASUREMENT_LOCATION_LEFT_WRIST ->
        concept(HealthConnectContract.SNOMED_CT, "5951000", "Structure of left wrist region")
    BloodPressureRecord.MEASUREMENT_LOCATION_RIGHT_WRIST ->
        concept(HealthConnectContract.SNOMED_CT, "9736006", "Structure of right wrist region")
    BloodPressureRecord.MEASUREMENT_LOCATION_LEFT_UPPER_ARM ->
        concept(HealthConnectContract.SNOMED_CT, "368208006", "Left upper arm structure")
    BloodPressureRecord.MEASUREMENT_LOCATION_RIGHT_UPPER_ARM ->
        concept(HealthConnectContract.SNOMED_CT, "368209003", "Right upper arm structure")
    else -> throw InvalidHealthConnectRecord("Unsupported Health Connect blood-pressure location: $value")
}

internal fun temperatureMeasurementLocation(value: Int): CodeableConcept? = when (value) {
    BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_UNKNOWN -> null
    BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_ARMPIT ->
        concept(HealthConnectContract.SNOMED_CT, "422543003", "Structure of axillary fossa")
    BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_FINGER ->
        concept(HealthConnectContract.SNOMED_CT, "7569003", "Finger structure")
    BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_FOREHEAD ->
        concept(HealthConnectContract.SNOMED_CT, "52795006", "Forehead structure")
    BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_MOUTH ->
        concept(HealthConnectContract.SNOMED_CT, "74262004", "Oral cavity structure")
    BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_RECTUM ->
        concept(HealthConnectContract.SNOMED_CT, "34402009", "Rectum structure")
    BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_TEMPORAL_ARTERY ->
        concept(HealthConnectContract.SNOMED_CT, "15672000", "Structure of superficial temporal artery")
    BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_TOE ->
        concept(HealthConnectContract.SNOMED_CT, "29707007", "Toe structure")
    BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_EAR ->
        concept(HealthConnectContract.SNOMED_CT, "117590005", "Ear structure")
    BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_WRIST ->
        concept(HealthConnectContract.SNOMED_CT, "8205005", "Wrist region structure")
    BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_VAGINA ->
        concept(HealthConnectContract.SNOMED_CT, "76784001", "Vaginal structure")
    else -> throw InvalidHealthConnectRecord("Unsupported Health Connect temperature location: $value")
}

internal fun skinTemperatureMeasurementLocation(value: Int): CodeableConcept? = when (value) {
    SkinTemperatureRecord.MEASUREMENT_LOCATION_UNKNOWN -> null
    SkinTemperatureRecord.MEASUREMENT_LOCATION_FINGER ->
        concept(HealthConnectContract.SNOMED_CT, "7569003", "Finger structure")
    SkinTemperatureRecord.MEASUREMENT_LOCATION_TOE ->
        concept(HealthConnectContract.SNOMED_CT, "29707007", "Toe structure")
    SkinTemperatureRecord.MEASUREMENT_LOCATION_WRIST ->
        concept(HealthConnectContract.SNOMED_CT, "8205005", "Wrist region structure")
    else -> throw InvalidHealthConnectRecord("Unsupported Health Connect skin-temperature location: $value")
}
