//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.metadata.Metadata
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.InstantType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Period
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

internal fun Observation.claimProfile(spec: MobileQuantitySpec) =
    claimProfile(spec.profile, spec.adapterSpecific)

internal fun Observation.claimProfile(spec: MobileCodedSpec) =
    claimProfile(spec.profile, spec.adapterSpecific)

internal fun Observation.claimProfile(profile: String, adapterSpecific: Boolean) {
    if (adapterSpecific) {
        claimAdapterSpecificProfile(profile)
    } else {
        claimMeasurementProfile(profile)
    }
}

@Suppress("LongParameterList")
internal fun HealthConnectConverter.convertInstantCoded(
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
    val resolvedContext = context.resolve(
        metadata,
        synchronizationScope.identityKey,
        bundleIdentifier(eventSequence),
    )
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
internal fun HealthConnectConverter.convertPeriodCoded(
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
    val resolvedContext = context.resolve(
        metadata,
        synchronizationScope.identityKey,
        bundleIdentifier(eventSequence),
    )
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

internal fun codedValue(sharedSystem: String, sourceSystem: String, coding: SourceCodedValue): CodeableConcept =
    CodeableConcept().apply {
        addCoding(Coding(sharedSystem, coding.sharedCode, coding.sharedDisplay))
        addCoding(Coding(sourceSystem, coding.sourceCode, coding.sourceDisplay))
    }

internal fun category(code: String): CodeableConcept = concept(
    HealthConnectContract.OBSERVATION_CATEGORY,
    code,
    when (code) {
        HealthConnectConverter.VITAL_SIGNS_CATEGORY -> "Vital Signs"
        HealthConnectConverter.LABORATORY_CATEGORY -> "Laboratory"
        HealthConnectConverter.ACTIVITY_CATEGORY -> "Activity"
        else -> code
    },
)

/** Keeps a stripped decimal in plain notation so a multiple of ten never gains an exponent. */
internal fun BigDecimal.withPlainScale(): BigDecimal = if (scale() < 0) setScale(0) else this

internal fun Double.fhirDecimal(
    field: String,
    valueDomain: QuantityValueDomain = QuantityValueDomain(),
    exactValue: BigDecimal? = null,
): BigDecimal {
    if (!isFinite()) throw InvalidHealthConnectRecord("$field must be finite.")
    return valueDomain.requireValue(exactValue ?: BigDecimal.valueOf(this), field)
}

internal fun mindfulnessDurationMinutes(start: Instant, end: Instant): BigDecimal {
    val nanos = ChronoUnit.NANOS.between(start, end)
    if (nanos <= 0L) {
        throw InvalidHealthConnectRecord("MindfulnessSessionRecord must have a positive interval.")
    }
    return BigDecimal.valueOf(nanos)
        .divide(HealthConnectConverter.NANOSECONDS_PER_MINUTE, HealthConnectConverter.SESSION_DURATION_SCALE, RoundingMode.HALF_EVEN)
        .stripTrailingZeros()
        .withPlainScale()
}

internal fun HealthConnectConverter.baseObservation(
    metadata: Metadata,
    sourceIdentity: HealthConnectSourceIdentity,
    outputIdentifier: Identifier?,
    resolvedContext: ResolvedFhirContext,
    singleMeasurementId: String? = null,
): Observation =
    Observation().apply {
        require((outputIdentifier == null) == (singleMeasurementId != null)) {
            "An Observation requires either an explicit multi-output identity or one exactly-one measurement id."
        }
        addIdentifier(sourceIdentity.identifier.copy())
        addIdentifier(
            outputIdentifier ?: HealthConnectIdentity.singleOutput(
                synchronizationScope.identityKey,
                sourceIdentity,
                requireNotNull(singleMeasurementId),
            ),
        )
        if (singleMeasurementId != null) {
            context.nativeIdentifierDisclosure?.identifier(
                nativeId = metadata.id,
                eventIdentifierSystem = context.eventIdentifierSystem,
                entryNodeIdentifierSystem = context.entryNodeIdentifierSystem,
                identityKey = synchronizationScope.identityKey,
            )?.let(::addIdentifier)
        }
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

internal fun Observation.claimMeasurementProfile(sharedProfile: String) {
    check(meta.profile.isEmpty()) { "Measurement profile claims must be assigned exactly once." }
    meta.addProfile(sharedProfile)
    meta.addProfile(HealthConnectContract.HEALTH_CONNECT_OBSERVATION_PROFILE)
}

internal fun Observation.claimAdapterSpecificProfile(adapterProfile: String) {
    check(adapterProfile in HealthConnectContract.adapterSpecificObservationProfiles) {
        "Only an admitted Health Connect-specific Observation profile may use this claim mode."
    }
    check(meta.profile.isEmpty()) { "Measurement profile claims must be assigned exactly once." }
    meta.addProfile(adapterProfile)
}
