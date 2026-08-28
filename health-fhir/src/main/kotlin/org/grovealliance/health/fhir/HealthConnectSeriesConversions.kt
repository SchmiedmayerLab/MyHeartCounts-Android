//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Period
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset

internal fun HealthConnectConverter.convertHeartRate(
    record: HeartRateRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion {
    val source = sourceIdentity(record.metadata, HealthConnectConverter.HEART_RATE_RECORD)
    val resolvedContext = context.resolve(
        record.metadata,
        synchronizationScope.identityKey,
        bundleIdentifier(eventSequence),
    )
    validate(record)

    // Assign occurrences in the exact platform list order before deterministic output sorting.
    // Clinical values are deliberately excluded so a corrected value retains its source slot.
    val identifiedSamples = assignSourceListOccurrences(record.samples) { it.time }
    val observations = identifiedSamples
        .sortedWith(compareBy({ it.first.time }, { it.second }))
        .map { (sample, occurrence) ->
            HealthConnectWireFormat.requireFhirInstant(sample.time, "Heart-rate sample time")
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
        HealthConnectConverter.HEART_RATE_RECORD,
        source,
        observations,
        convertedAt,
        eventSequence,
        resolvedContext,
    )
}

internal fun HealthConnectConverter.convertSteps(
    record: StepsRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion {
    val source = sourceIdentity(record.metadata, HealthConnectConverter.STEPS_RECORD)
    val resolvedContext = context.resolve(
        record.metadata,
        synchronizationScope.identityKey,
        bundleIdentifier(eventSequence),
    )
    if (!record.startTime.isBefore(record.endTime)) {
        throw InvalidHealthConnectRecord("StepsRecord must have a positive interval.")
    }
    val count = HealthConnectContract.quantityValueDomains.getValue("step-count")
        .requireValue(record.count.toBigDecimal(), "StepsRecord count")
    val observation = baseObservation(
        record.metadata,
        source,
        null,
        resolvedContext,
        "step-count",
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
        value = quantity(count, "{steps}", "steps")
    }
    return conversion(
        record.metadata,
        HealthConnectConverter.STEPS_RECORD,
        source,
        listOf(observation),
        convertedAt,
        eventSequence,
        resolvedContext,
    )
}

internal fun HealthConnectConverter.convertWeight(
    record: WeightRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion {
    val source = sourceIdentity(record.metadata, HealthConnectConverter.WEIGHT_RECORD)
    val resolvedContext = context.resolve(
        record.metadata,
        synchronizationScope.identityKey,
        bundleIdentifier(eventSequence),
    )
    val kilograms = record.weight.inKilograms
    if (!kilograms.isFinite() || kilograms < 0.0 || kilograms > HealthConnectConverter.MAX_WEIGHT_KILOGRAMS) {
        throw InvalidHealthConnectRecord("WeightRecord must contain a finite weight in [0, 1000] kg.")
    }
    val observation = baseObservation(
        record.metadata,
        source,
        null,
        resolvedContext,
        "body-weight",
    ).apply {
        claimMeasurementProfile(HealthConnectContract.MOBILE_BODY_WEIGHT_PROFILE)
        addCategory(concept(HealthConnectContract.OBSERVATION_CATEGORY, "vital-signs", "Vital Signs"))
        code = concept(HealthConnectContract.LOINC, "29463-7", "Body weight")
        effective = DateTimeType(record.time.fhirDateTime(record.zoneOffset, "Weight time"))
        value = quantity(BigDecimal.valueOf(kilograms), "kg", "kg")
    }
    return conversion(
        record.metadata,
        HealthConnectConverter.WEIGHT_RECORD,
        source,
        listOf(observation),
        convertedAt,
        eventSequence,
        resolvedContext,
    )
}

@Suppress("LongParameterList")
internal fun HealthConnectConverter.convertInstantQuantity(
    metadata: Metadata,
    recordType: String,
    time: Instant,
    offset: ZoneOffset?,
    value: Double,
    spec: MobileQuantitySpec,
    bodySite: CodeableConcept? = null,
    method: CodeableConcept? = null,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion {
    val source = sourceIdentity(metadata, recordType)
    val resolvedContext = context.resolve(
        metadata,
        synchronizationScope.identityKey,
        bundleIdentifier(eventSequence),
    )
    val decimal = value.fhirDecimal(spec.display, spec.valueDomain)
    val observation = baseObservation(
        metadata,
        source,
        null,
        resolvedContext,
        spec.measurement,
    ).apply {
        claimProfile(spec)
        spec.category?.let { addCategory(category(it)) }
        code = concept(spec.codeSystem, spec.code, spec.display)
        effective = DateTimeType(time.fhirDateTime(offset, "${spec.display} time"))
        this.value = quantity(decimal, spec.unitCode, spec.unitDisplay)
        this.bodySite = bodySite
        this.method = method
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
internal fun HealthConnectConverter.convertIntervalQuantity(
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
    val resolvedContext = context.resolve(
        metadata,
        synchronizationScope.identityKey,
        bundleIdentifier(eventSequence),
    )
    val decimal = value.fhirDecimal(spec.display, spec.valueDomain, exactValue)
    val observation = baseObservation(
        metadata,
        source,
        null,
        resolvedContext,
        spec.measurement,
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
internal fun HealthConnectConverter.convertSampleSeries(
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
    val resolvedContext = context.resolve(
        metadata,
        synchronizationScope.identityKey,
        bundleIdentifier(eventSequence),
    )
    // The adapter preserves the platform list through SeriesSample. Occurrences therefore bind
    // equal-time samples to source slots before deterministic output sorting changes their order.
    val identifiedSamples = assignSourceListOccurrences(samples) { it.time }
    val observations = identifiedSamples
        .sortedWith(compareBy({ it.first.time }, { it.second }))
        .map { (sample, occurrence) ->
            HealthConnectWireFormat.requireFhirInstant(sample.time, "${spec.display} sample time")
            val decimal = sample.value.fhirDecimal("${spec.display} sample value", spec.valueDomain)
            val output = HealthConnectIdentity.seriesSampleOutput(
                synchronizationScope.identityKey,
                source,
                sample.time,
                occurrence,
            )
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
