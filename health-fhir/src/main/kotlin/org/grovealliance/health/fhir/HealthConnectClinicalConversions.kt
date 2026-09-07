//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import org.grovealliance.health.RecordType
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Specimen
import java.time.Instant

internal fun HealthConnectConverter.convertBloodGlucose(
    record: BloodGlucoseRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion {
    val source = sourceIdentity(record.metadata, RecordType.bloodGlucose.identifier)
    val resolvedContext = context.resolve(
        record.metadata,
        synchronizationScope.identityKey,
        bundleIdentifier(eventSequence),
    )
    val definition = bloodGlucoseDefinition(record.specimenSource)
    val specimenIdentity = specimenIdentifier(source, definition.specimenSourceCode)
    val specimen = Specimen().apply {
        meta.addProfile(HealthConnectContract.HEALTH_CONNECT_SPECIMEN_PROFILE)
        addIdentifier(source.identifier.copy())
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
        definition.measurement,
    ).apply {
        claimAdapterSpecificProfile(definition.profile)
        addCategory(category(HealthConnectConverter.LABORATORY_CATEGORY))
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
        RecordType.bloodGlucose.identifier,
        source,
        listOf(observation),
        convertedAt,
        eventSequence,
        resolvedContext,
        listOf(specimenResource),
    )
}

internal fun HealthConnectConverter.convertBloodPressure(
    record: BloodPressureRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion {
    val source = sourceIdentity(record.metadata, RecordType.bloodPressure.identifier)
    val resolvedContext = context.resolve(
        record.metadata,
        synchronizationScope.identityKey,
        bundleIdentifier(eventSequence),
    )
    val systolic = record.systolic.inMillimetersOfMercury.fhirDecimal("Blood-pressure systolic value")
    val diastolic = record.diastolic.inMillimetersOfMercury.fhirDecimal("Blood-pressure diastolic value")
    val observation = baseObservation(
        record.metadata,
        source,
        null,
        resolvedContext,
        "blood-pressure",
    ).apply {
        claimMeasurementProfile(HealthConnectContract.MOBILE_BLOOD_PRESSURE_PROFILE)
        addCategory(concept(HealthConnectContract.OBSERVATION_CATEGORY, HealthConnectConverter.VITAL_SIGNS_CATEGORY, "Vital Signs"))
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
        RecordType.bloodPressure.identifier,
        source,
        listOf(observation),
        convertedAt,
        eventSequence,
        resolvedContext,
    )
}

internal fun HealthConnectConverter.convertBasalBodyTemperature(
    record: BasalBodyTemperatureRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertInstantQuantity(
    metadata = record.metadata,
    recordType = RecordType.basalBodyTemperature.identifier,
    time = record.time,
    offset = record.zoneOffset,
    value = record.temperature.inCelsius,
    spec = MobileQuantitySpec(
        profile = HealthConnectContract.MOBILE_BASAL_BODY_TEMPERATURE_PROFILE,
        category = HealthConnectConverter.VITAL_SIGNS_CATEGORY,
        codeSystem = HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
        code = "basal-body-temperature",
        display = "Basal body temperature",
        unitCode = "Cel",
        unitDisplay = "Cel",
        valueDomain = QuantityValueDomain.UNBOUNDED,
    ),
    bodySite = temperatureMeasurementLocation(record.measurementLocation),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)
internal fun HealthConnectConverter.convertBodyTemperature(
    record: BodyTemperatureRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertInstantQuantity(
    metadata = record.metadata,
    recordType = RecordType.bodyTemperature.identifier,
    time = record.time,
    offset = record.zoneOffset,
    value = record.temperature.inCelsius,
    spec = MobileQuantitySpec(
        profile = HealthConnectContract.MOBILE_BODY_TEMPERATURE_PROFILE,
        category = HealthConnectConverter.VITAL_SIGNS_CATEGORY,
        codeSystem = HealthConnectContract.LOINC,
        code = "8310-5",
        display = "Body temperature",
        unitCode = "Cel",
        unitDisplay = "Cel",
        valueDomain = QuantityValueDomain.UNBOUNDED,
    ),
    bodySite = temperatureMeasurementLocation(record.measurementLocation),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertDistance(
    record: DistanceRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertIntervalQuantity(
    metadata = record.metadata,
    recordType = RecordType.distance.identifier,
    start = record.startTime,
    startOffset = record.startZoneOffset,
    end = record.endTime,
    endOffset = record.endZoneOffset,
    value = record.distance.inMeters,
    spec = MobileQuantitySpec(
        profile = HealthConnectContract.MOBILE_DISTANCE_PROFILE,
        category = HealthConnectConverter.ACTIVITY_CATEGORY,
        codeSystem = HealthConnectContract.LOINC,
        code = "103208-5",
        display = "Distance traveled",
        unitCode = "m",
        unitDisplay = "m",
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertHeight(
    record: HeightRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertInstantQuantity(
    metadata = record.metadata,
    recordType = RecordType.height.identifier,
    time = record.time,
    offset = record.zoneOffset,
    value = record.height.inMeters * HealthConnectConverter.CENTIMETERS_PER_METER,
    spec = MobileQuantitySpec(
        profile = HealthConnectContract.MOBILE_BODY_HEIGHT_PROFILE,
        category = HealthConnectConverter.VITAL_SIGNS_CATEGORY,
        codeSystem = HealthConnectContract.LOINC,
        code = "8302-2",
        display = "Body height",
        unitCode = "cm",
        unitDisplay = "cm",
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertOxygenSaturation(
    record: OxygenSaturationRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertInstantQuantity(
    metadata = record.metadata,
    recordType = RecordType.oxygenSaturation.identifier,
    time = record.time,
    offset = record.zoneOffset,
    value = record.percentage.value,
    spec = MobileQuantitySpec(
        profile = HealthConnectContract.MOBILE_OXYGEN_SATURATION_PROFILE,
        category = HealthConnectConverter.VITAL_SIGNS_CATEGORY,
        codeSystem = HealthConnectContract.LOINC,
        code = "2708-6",
        display = "Oxygen saturation in Arterial blood",
        unitCode = "%",
        unitDisplay = "%",
        valueDomain = HealthConnectContract.quantityValueDomains.getValue("oxygen-saturation"),
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)

internal fun HealthConnectConverter.convertRespiratoryRate(
    record: RespiratoryRateRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion = convertInstantQuantity(
    metadata = record.metadata,
    recordType = RecordType.respiratoryRate.identifier,
    time = record.time,
    offset = record.zoneOffset,
    value = record.rate,
    spec = MobileQuantitySpec(
        profile = HealthConnectContract.MOBILE_RESPIRATORY_RATE_PROFILE,
        category = HealthConnectConverter.VITAL_SIGNS_CATEGORY,
        codeSystem = HealthConnectContract.LOINC,
        code = "9279-1",
        display = "Respiratory rate",
        unitCode = "/min",
        unitDisplay = "breaths/minute",
    ),
    convertedAt = convertedAt,
    eventSequence = eventSequence,
)
