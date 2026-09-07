//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import org.hl7.fhir.r4.model.Identifier
import java.security.MessageDigest
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Base64

/** Domain-separated opaque identities defined by the Grove FHIR contracts. */
@Suppress("TooManyFunctions")
internal object HealthConnectIdentity {
    fun record(
        key: GroveHmacIdentityKey,
        repositoryScope: FhirIdentifierKey,
        recordType: String,
        rawRecordId: String,
    ): HealthConnectSourceIdentity {
        require(recordType in HealthConnectCatalog.allRecordTypeIdentifiers) {
            "Record type must belong to the closed Health Connect 1.1.0 inventory."
        }
        require(rawRecordId.isNotEmpty()) { "Health Connect raw record id must not be empty." }
        return HealthConnectSourceIdentity(
            adapterId = ADAPTER_ID,
            sourceType = recordType,
            repositoryScope = repositoryScope,
            nativeRecordId = rawRecordId,
            identifier = key.identifier(
                GroveOpaqueIdentityKind.SOURCE_RECORD,
                ADAPTER_ID,
                recordType,
                repositoryScope.system,
                repositoryScope.value,
                rawRecordId,
            ),
        )
    }

    /** A Device instance requires a governed per-unit token; model/manufacturer are not identity. */
    fun recordingDevice(
        key: GroveHmacIdentityKey,
        subjectKey: FhirIdentifierKey,
        stablePerUnitToken: String,
    ): Identifier {
        require(stablePerUnitToken.isNotBlank()) {
            "A recording Device instance requires a stable, explicitly governed per-unit token."
        }
        return key.identifier(
            GroveOpaqueIdentityKind.RECORDING_DEVICE,
            ADAPTER_ID,
            subjectKey.system,
            subjectKey.value,
            stablePerUnitToken,
        )
    }

    /** Event-bound immutable application/host facts; never a mutable long-lived Device identity. */
    fun deviceSnapshot(
        key: GroveHmacIdentityKey,
        event: Identifier,
        deviceRole: String,
        sourceDeviceToken: String,
    ): Identifier {
        require(event.hasSystem() && event.hasValue() && event.hasGroveRole(GroveIdentifierRole.EVENT)) {
            "A Device snapshot requires its complete typed exchange-event Identifier."
        }
        require(deviceRole.matches(DEVICE_ROLE)) {
            "A Device snapshot role must use the closed lowercase token grammar."
        }
        GroveUnicode.requireScalarText(sourceDeviceToken, "Device snapshot source token")
        require(sourceDeviceToken.isNotBlank()) { "A Device snapshot requires a nonblank source token." }
        return key.identifier(
            GroveOpaqueIdentityKind.DEVICE_SNAPSHOT,
            event.system,
            event.value,
            deviceRole,
            sourceDeviceToken,
        )
    }

    fun writerRecord(
        key: GroveHmacIdentityKey,
        writerApplication: FhirIdentifierKey,
        clientRecordId: String,
    ): Identifier = key.identifier(
        GroveOpaqueIdentityKind.WRITER_RECORD,
        writerApplication.system,
        writerApplication.value,
        clientRecordId,
    )

    fun sampleOutput(
        key: GroveHmacIdentityKey,
        source: HealthConnectSourceIdentity,
        sampleTime: Instant,
        duplicateOccurrence: Int,
    ): Identifier {
        require(duplicateOccurrence >= 0) { "Sample identity requires an unsigned duplicate occurrence." }
        return key.identifier(
            GroveOpaqueIdentityKind.SOURCE_OUTPUT,
            source.components + listOf(
                SAMPLE_OUTPUT_ROLE,
                listOf(sampleTime.utc9(), duplicateOccurrence.toString())
                    .joinToString(OUTPUT_DISCRIMINATOR_SEPARATOR),
            ),
        )
    }

    fun nutrientOutput(
        key: GroveHmacIdentityKey,
        source: HealthConnectSourceIdentity,
        nutrientToken: String,
    ): Identifier {
        require(nutrientToken in NUTRIENT_TOKENS) {
            "Nutrient identity requires an admitted Health Connect dietary measurement token."
        }
        return key.identifier(
            GroveOpaqueIdentityKind.SOURCE_OUTPUT,
            source.components + listOf(PRESENT_FIELD_OUTPUT_ROLE, nutrientToken),
        )
    }

    fun sleepStageOutput(
        key: GroveHmacIdentityKey,
        source: HealthConnectSourceIdentity,
        start: Instant,
        end: Instant,
        sourceStageToken: String,
        duplicateOccurrence: Int,
    ): Identifier {
        require(sourceStageToken in SLEEP_STAGE_TOKENS) {
            "Sleep-stage identity requires an exact Health Connect 1.1.0 stage token."
        }
        require(duplicateOccurrence >= 0) { "Sleep-stage identity requires an unsigned duplicate occurrence." }
        return key.identifier(
            GroveOpaqueIdentityKind.SOURCE_OUTPUT,
            source.components + listOf(
                SLEEP_STAGE_OUTPUT_ROLE,
                listOf(start.utc9(), end.utc9(), sourceStageToken, duplicateOccurrence.toString())
                    .joinToString(OUTPUT_DISCRIMINATOR_SEPARATOR),
            ),
        )
    }

    fun segmentOutput(
        key: GroveHmacIdentityKey,
        source: HealthConnectSourceIdentity,
        start: Instant,
        end: Instant,
        sourceSegmentToken: String,
        duplicateOccurrence: Int,
    ): Identifier {
        require(sourceSegmentToken in HealthConnectWorkoutVocabulary.segmentIdentityTokens) {
            "Workout-segment identity requires an exact Health Connect 1.1.0 segment token."
        }
        require(duplicateOccurrence >= 0) { "Workout-segment identity requires an unsigned duplicate occurrence." }
        return key.identifier(
            GroveOpaqueIdentityKind.SOURCE_OUTPUT,
            source.components + listOf(
                WORKOUT_SEGMENT_OUTPUT_ROLE,
                listOf(start.utc9(), end.utc9(), sourceSegmentToken, duplicateOccurrence.toString())
                    .joinToString(OUTPUT_DISCRIMINATOR_SEPARATOR),
            ),
        )
    }

    fun specimenOutput(
        key: GroveHmacIdentityKey,
        source: HealthConnectSourceIdentity,
        sourceSpecimenToken: String,
    ): Identifier {
        require(sourceSpecimenToken in SPECIMEN_TOKENS) {
            "Specimen identity requires an admitted Health Connect specimen token."
        }
        return key.identifier(
            GroveOpaqueIdentityKind.SOURCE_OUTPUT,
            source.components + listOf(SPECIMEN_OUTPUT_ROLE, sourceSpecimenToken),
        )
    }

    fun singleOutput(
        key: GroveHmacIdentityKey,
        source: HealthConnectSourceIdentity,
        measurementId: String,
    ): Identifier {
        require(MEASUREMENT_ID.matches(measurementId)) {
            "An exactly-one output requires its canonical measurement id."
        }
        return key.identifier(
            GroveOpaqueIdentityKind.SOURCE_OUTPUT,
            source.components + listOf(SINGLE_OUTPUT_ROLE, measurementId),
        )
    }

    fun exchange(
        eventSystem: String,
        producerInstance: String,
        eventSequence: EventSequence,
    ): Identifier {
        requireAbsoluteSystem(eventSystem, "event Identifier system")
        require(PRODUCER_INSTANCE.matches(producerInstance)) {
            "Producer instance must use canonical lowercase RFC 4122 UUID text."
        }
        return Identifier().apply {
            system = eventSystem
            value = "${HealthConnectContract.EVENT_IDENTITY_PREFIX}:$producerInstance:${eventSequence.value}"
            type = org.hl7.fhir.r4.model.CodeableConcept(
                org.hl7.fhir.r4.model.Coding(
                    HealthConnectContract.GROVE_IDENTIFIER_ROLE,
                    GroveIdentifierRole.EVENT.code,
                    GroveIdentifierRole.EVENT.display,
                ),
            )
        }
    }

    fun conversionNode(
        entryNodeSystem: String,
        event: Identifier,
    ): Identifier = eventNode(entryNodeSystem, event, "conversion-provenance", 0)

    fun retractionNode(
        entryNodeSystem: String,
        event: Identifier,
    ): Identifier = eventNode(entryNodeSystem, event, "retraction-provenance", 0)

    /** Names a resource without a protocol-selected business identity inside one immutable event. */
    fun contextNode(
        entryNodeSystem: String,
        event: Identifier,
        resourceRole: String,
        ordinal: Int,
    ): Identifier = eventNode(entryNodeSystem, event, resourceRole, ordinal)

    private fun eventNode(
        entryNodeSystem: String,
        event: Identifier,
        resourceRole: String,
        ordinal: Int,
    ): Identifier {
        requireAbsoluteSystem(entryNodeSystem, "entry-node Identifier system")
        require(event.hasSystem() && event.hasValue() && event.hasGroveRole(GroveIdentifierRole.EVENT)) {
            "An entry node requires its complete typed exchange-event Identifier."
        }
        require(resourceRole.matches(Regex("[a-z][a-z0-9-]*")) && ordinal >= 0) {
            "Entry-node role and ordinal must use the closed lexical grammar."
        }
        val digest = framedSha256(
            listOf(
                HealthConnectContract.ENTRY_NODE_DOMAIN,
                event.system,
                event.value,
                resourceRole,
                ordinal.toString(),
            ),
        )
        return Identifier().apply {
            system = entryNodeSystem
            value = "${HealthConnectContract.ENTRY_NODE_IDENTITY_PREFIX}:$resourceRole:$ordinal:$digest"
            type = org.hl7.fhir.r4.model.CodeableConcept(
                org.hl7.fhir.r4.model.Coding(
                    HealthConnectContract.GROVE_IDENTIFIER_ROLE,
                    GroveIdentifierRole.ENTRY_NODE.code,
                    GroveIdentifierRole.ENTRY_NODE.display,
                ),
            )
        }
    }

    private fun framedSha256(fields: List<String>): String {
        val preimage = GroveExchangeProtocol.frameFields(fields)
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(MessageDigest.getInstance("SHA-256").digest(preimage))
    }

    private fun requireAbsoluteSystem(system: String, field: String) {
        GroveUnicode.requireScalarText(system, field)
        require(system.isAbsoluteAsciiUri()) {
            "$field must be a deployment-owned absolute ASCII RFC 3986 URI."
        }
    }

    private fun Instant.utc9(): String {
        HealthConnectWireFormat.requireFhirInstant(this, "Health Connect identity instant")
        return UTC_NANOSECOND.format(this)
    }

    private val UTC_NANOSECOND: DateTimeFormatter =
        DateTimeFormatterBuilder().appendInstant(INSTANT_FRACTION_DIGITS).toFormatter()
    private const val PRODUCER_INSTANCE_PATTERN =
        "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"
    private val PRODUCER_INSTANCE = Regex(PRODUCER_INSTANCE_PATTERN)

    /** Lexical form of a clear event Bundle identifier value, minted by [exchange]. */
    internal val EVENT_IDENTITY_VALUE = Regex(
        "${HealthConnectContract.EVENT_IDENTITY_PREFIX}:$PRODUCER_INSTANCE_PATTERN:[1-9][0-9]*",
    )

    /** Lexical form of a deterministic entry-node identifier value, minted by [contextNode]. */
    internal val ENTRY_NODE_IDENTITY_VALUE = Regex(
        "${HealthConnectContract.ENTRY_NODE_IDENTITY_PREFIX}:[a-z][a-z0-9-]*:(0|[1-9][0-9]*):[A-Za-z0-9_-]{43}",
    )
    private val DEVICE_ROLE = Regex("[a-z][a-z0-9-]*")
    private val MEASUREMENT_ID = Regex("[a-z][a-z0-9-]*")
    private const val ADAPTER_ID = "health-connect"
    private const val OUTPUT_DISCRIMINATOR_SEPARATOR = "|"
    private const val SINGLE_OUTPUT_ROLE = "single"
    private const val SAMPLE_OUTPUT_ROLE = "sample"
    private const val SLEEP_STAGE_OUTPUT_ROLE = "sleep-stage"
    private const val PRESENT_FIELD_OUTPUT_ROLE = "present-field"
    private const val SPECIMEN_OUTPUT_ROLE = "specimen"
    private const val WORKOUT_SEGMENT_OUTPUT_ROLE = "workout-segment"

    private val NUTRIENT_TOKENS =
        HealthConnectContract.mobileDietaryProfiles.keys + setOf(
            "dietary-energy-from-fat",
            "dietary-fat-trans",
            "dietary-fat-unsaturated",
            "dietary-folic-acid",
        )

    private val SLEEP_STAGE_TOKENS = setOf(
        "STAGE_TYPE_UNKNOWN",
        "STAGE_TYPE_AWAKE",
        "STAGE_TYPE_SLEEPING",
        "STAGE_TYPE_OUT_OF_BED",
        "STAGE_TYPE_LIGHT",
        "STAGE_TYPE_DEEP",
        "STAGE_TYPE_REM",
        "STAGE_TYPE_AWAKE_IN_BED",
    )
    private val SPECIMEN_TOKENS = setOf(
        "SPECIMEN_SOURCE_WHOLE_BLOOD",
        "SPECIMEN_SOURCE_CAPILLARY_BLOOD",
        "SPECIMEN_SOURCE_PLASMA",
        "SPECIMEN_SOURCE_SERUM",
        "SPECIMEN_SOURCE_INTERSTITIAL_FLUID",
    )
    private const val INSTANT_FRACTION_DIGITS = 9
}

/** The exact catalog components and opaque identifier for one source record. */
internal data class HealthConnectSourceIdentity(
    val adapterId: String,
    val sourceType: String,
    val repositoryScope: FhirIdentifierKey,
    val nativeRecordId: String,
    val identifier: Identifier,
) {
    val components: List<String>
        get() = listOf(
            adapterId,
            sourceType,
            repositoryScope.system,
            repositoryScope.value,
            nativeRecordId,
        )

    override fun toString(): String =
        "HealthConnectSourceIdentity(" +
            "adapterId=$adapterId, sourceType=$sourceType, " +
            "repositoryScope=$repositoryScope, nativeRecordId=<redacted>, identifier=<redacted>)"
}
