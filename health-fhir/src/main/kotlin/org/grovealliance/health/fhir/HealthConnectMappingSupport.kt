//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Quantity
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneOffset

internal fun requireSourceScalarText(value: String, field: String): String = try {
    GroveUnicode.requireScalarText(value, field)
} catch (error: IllegalArgumentException) {
    throw InvalidHealthConnectRecord(requireNotNull(error.message), error)
}

/** Validates the all-or-nothing writer identity represented by AndroidX Metadata. */
internal fun Metadata.validatedClientRecordId(): String? {
    val id = clientRecordId ?: return null
    if (id.isBlank()) {
        throw InvalidHealthConnectRecord(
            "Health Connect metadata.clientRecordId must not be blank when present.",
        )
    }
    requireSourceScalarText(id, "Health Connect metadata.clientRecordId")
    if (clientRecordVersion < 0) {
        throw InvalidHealthConnectRecord(
            "Health Connect metadata.clientRecordVersion must be non-negative when clientRecordId is present.",
        )
    }
    return id
}

internal fun validate(record: HeartRateRecord) {
    val invalidReason = when {
        record.startTime.isAfter(record.endTime) ->
            "HeartRateRecord startTime must not be after endTime."
        record.samples.any { it.time < record.startTime || it.time > record.endTime } ->
            "Heart-rate sample lies outside its source record interval."
        else -> null
    }
    invalidReason?.let { throw InvalidHealthConnectRecord(it) }
}

internal fun HealthConnectConverter.sampleIdentifier(
    sourceIdentifier: HealthConnectSourceIdentity,
    sample: HeartRateRecord.Sample,
    occurrence: Int,
): Identifier = HealthConnectIdentity.heartRateSampleOutput(
    synchronizationScope.identityKey,
    sourceIdentifier,
    sample.time,
    occurrence,
)

internal fun HealthConnectConverter.sleepStageIdentifier(
    sourceIdentifier: HealthConnectSourceIdentity,
    stage: SleepSessionRecord.Stage,
    occurrence: Int,
): Identifier = HealthConnectIdentity.sleepStageOutput(
    synchronizationScope.identityKey,
    sourceIdentifier,
    stage.startTime,
    stage.endTime,
    sleepStageCoding(stage.stage).sourceCode,
    occurrence,
)

@Suppress("LongMethod")
internal fun bloodGlucoseDefinition(specimenSource: Int): BloodGlucoseDefinition = when (specimenSource) {
    BloodGlucoseRecord.SPECIMEN_SOURCE_WHOLE_BLOOD -> BloodGlucoseDefinition(
        measurement = "blood-glucose",
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
        measurement = "capillary-blood-glucose",
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
        measurement = "serum-plasma-glucose",
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
        measurement = "serum-plasma-glucose",
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
        measurement = "interstitial-glucose",
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
        "Health Connect tear glucose has no admitted shared Grove Mobile profile in 0.6.0.",
    )
    BloodGlucoseRecord.SPECIMEN_SOURCE_UNKNOWN -> throw InvalidHealthConnectRecord(
        "Health Connect blood glucose requires an explicit supported specimen source.",
    )
    else -> throw InvalidHealthConnectRecord(
        "Unsupported Health Connect blood-glucose specimen source: $specimenSource",
    )
}

internal fun HealthConnectConverter.specimenIdentifier(
    sourceIdentifier: HealthConnectSourceIdentity,
    specimenSourceCode: String,
): Identifier =
    HealthConnectIdentity.specimenOutput(
        synchronizationScope.identityKey,
        sourceIdentifier,
        specimenSourceCode,
    )

internal fun HealthConnectConverter.glucoseMealContext(record: BloodGlucoseRecord): Extension? {
    val relation = bloodGlucoseRelationToMeal(record.relationToMeal)
    val meal = bloodGlucoseMealType(record.mealType)
    if (relation == null && meal == null) return null
    return Extension(HealthConnectContract.HEALTH_CONNECT_GLUCOSE_MEAL_CONTEXT).apply {
        relation?.let { addExtension(Extension("relationToMeal", it)) }
        meal?.let { addExtension(Extension("mealType", it)) }
    }
}

internal fun HealthConnectConverter.recordingMethod(metadata: Metadata): Extension? {
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

internal fun quantity(value: BigDecimal, code: String, unit: String): Quantity =
    Quantity().setValue(value).setSystem(HealthConnectContract.UCUM).setCode(code).setUnit(unit)

internal fun Instant.fhirDateTime(zoneOffset: ZoneOffset?, field: String): String {
    HealthConnectWireFormat.requireFhirInstant(this, field)
    val canonical = mobileEffectiveInstant()
    HealthConnectWireFormat.requireFhirInstant(
        canonical,
        "$field after Mobile millisecond canonicalization",
    )
    if (zoneOffset == null) return canonical.toString()
    val fhirOffsetRange =
        -HealthConnectConverter.MAX_FHIR_OFFSET_SECONDS..HealthConnectConverter.MAX_FHIR_OFFSET_SECONDS
    if (
        zoneOffset.totalSeconds % HealthConnectConverter.SECONDS_PER_MINUTE != 0 ||
        zoneOffset.totalSeconds !in fhirOffsetRange
    ) {
        throw InvalidHealthConnectRecord(
            "$field offset must use whole minutes in the FHIR range -14:00 through +14:00.",
        )
    }
    val local = canonical.atOffset(zoneOffset)
    if (local.year !in HealthConnectConverter.MIN_FHIR_YEAR..HealthConnectConverter.MAX_FHIR_YEAR) {
        throw InvalidHealthConnectRecord("$field must retain a four-digit FHIR year after applying its offset.")
    }
    return HealthConnectConverter.FHIR_OFFSET_DATE_TIME.format(local)
}

/** Applies the source-neutral Mobile effective-time policy without altering identity instants. */
internal fun Instant.mobileEffectiveInstant(): Instant {
    val exactEpochMilliseconds = BigDecimal.valueOf(epochSecond)
        .multiply(HealthConnectConverter.MILLISECONDS_PER_SECOND)
        .add(BigDecimal.valueOf(nano.toLong(), HealthConnectConverter.NANOSECONDS_TO_MILLISECONDS_SCALE))
    return Instant.ofEpochMilli(
        exactEpochMilliseconds.setScale(0, RoundingMode.HALF_EVEN).longValueExact(),
    )
}
