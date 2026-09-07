//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.hl7.fhir.r4.formats.IParser
import org.hl7.fhir.r4.formats.JsonParser
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Parameters
import java.time.Instant

/**
 * Lossless storage codec for Room rows; the pending Bundle JSON remains the authoritative wire bytes.
 *
 * The composed JSON is invisible to the exported Room schema, so the Room schema version doubles as
 * this blob's format version: any field added, renamed, or removed below requires a schema bump and
 * a migration that rewrites every stored row. Version 1 has never shipped, so the fields it names
 * are still free to move without one.
 */
internal object RoomHealthConnectJournalCodec {
    private val json = Json {
        ignoreUnknownKeys = false
        isLenient = false
    }

    fun encodeEntry(entry: HealthConnectExportJournalEntry): String = entryJson(entry).toString()

    fun decodeEntry(encoded: String): HealthConnectExportJournalEntry = decodeEntry(
        json.parseToJsonElement(encoded).jsonObject,
    )

    fun encodePending(pending: HealthConnectPendingExport): String = buildJsonObject {
        put("eventSequence", JsonPrimitive(pending.eventSequence.value))
        put("baseRevision", pending.baseRevision?.value?.let(::JsonPrimitive) ?: JsonNull)
        put("repositoryScopeKey", JsonPrimitive(pending.repositoryScopeKey.value))
        put("projectionScopeKey", JsonPrimitive(pending.projectionScopeKey.value))
        put("operation", JsonPrimitive(pending.operation.wireValue))
        put("recordType", JsonPrimitive(pending.recordType))
        put("healthConnectId", JsonPrimitive(pending.healthConnectId))
        put("sourceRecordIdentifier", JsonPrimitive(identifierJson(pending.sourceRecordIdentifier)))
        put("sourceVersion", JsonPrimitive(pending.sourceVersion.toString()))
        put("bundleJson", JsonPrimitive(pending.bundleJson))
        put("payloadSha256", JsonPrimitive(pending.payloadSha256))
        put("retractedTargets", targetJson(pending.retractedTargets))
        put("nextEntry", entryJson(pending.nextEntry))
    }.toString()

    fun decodePending(encoded: String): HealthConnectPendingExport {
        val value = json.parseToJsonElement(encoded).jsonObject
        val bundleJson = value.string("bundleJson")
        return HealthConnectPendingExport(
            eventSequence = EventSequence(value.string("eventSequence")),
            baseRevision = value.nullableString("baseRevision")?.let(::HealthConnectJournalRevision),
            repositoryScopeKey = ScopeKey(value.string("repositoryScopeKey")),
            projectionScopeKey = ScopeKey(value.string("projectionScopeKey")),
            operation = HealthConnectExportOperation.entries.single {
                it.wireValue == value.string("operation")
            },
            recordType = value.string("recordType"),
            healthConnectId = value.string("healthConnectId"),
            sourceRecordIdentifier = parseIdentifier(value.string("sourceRecordIdentifier")),
            sourceVersion = Instant.parse(value.string("sourceVersion")),
            bundle = parseBundle(bundleJson),
            bundleJson = bundleJson,
            payloadSha256 = value.string("payloadSha256"),
            retractedTargets = parseTargets(value.array("retractedTargets")),
            nextEntry = decodeEntry(value.objectValue("nextEntry")),
        )
    }

    private fun entryJson(entry: HealthConnectExportJournalEntry): JsonObject = buildJsonObject {
        put("repositoryScopeKey", JsonPrimitive(entry.repositoryScopeKey.value))
        put("projectionScopeKey", JsonPrimitive(entry.projectionScopeKey.value))
        put("recordType", JsonPrimitive(entry.recordType))
        put("healthConnectId", JsonPrimitive(entry.healthConnectId))
        put("dataOriginPackage", JsonPrimitive(entry.dataOriginPackage))
        put("sourceLastModified", JsonPrimitive(entry.sourceLastModified.toString()))
        put("conversionContractMarker", JsonPrimitive(entry.conversionContractMarker))
        put("sourceRecordIdentifier", JsonPrimitive(identifierJson(entry.sourceRecordIdentifier)))
        put("bundleJson", JsonPrimitive(HealthConnectWireFormat.bundleJson(entry.bundle)))
        put("destinationReferences", destinationReferencesJson(entry.destinationReferences))
        put("lastEventSequence", entry.lastEventSequence?.value?.let(::JsonPrimitive) ?: JsonNull)
        put("state", JsonPrimitive(entry.state.name))
        put("invalidatedAt", entry.invalidatedAt?.toString()?.let(::JsonPrimitive) ?: JsonNull)
    }

    private fun decodeEntry(value: JsonObject): HealthConnectExportJournalEntry {
        val bundle = parseBundle(value.string("bundleJson"))
        return HealthConnectExportJournalEntry(
            repositoryScopeKey = ScopeKey(value.string("repositoryScopeKey")),
            projectionScopeKey = ScopeKey(value.string("projectionScopeKey")),
            recordType = value.string("recordType"),
            healthConnectId = value.string("healthConnectId"),
            dataOriginPackage = value.string("dataOriginPackage"),
            sourceLastModified = Instant.parse(value.string("sourceLastModified")),
            conversionContractMarker = value.string("conversionContractMarker"),
            sourceRecordIdentifier = parseIdentifier(value.string("sourceRecordIdentifier")),
            bundle = bundle,
            destinationReferences = parseDestinationReferences(value.array("destinationReferences")),
            lastEventSequence = value.nullableString("lastEventSequence")?.let(::EventSequence),
            state = HealthConnectExportState.valueOf(value.string("state")),
            invalidatedAt = value.nullableString("invalidatedAt")?.let(Instant::parse),
        )
    }

    private fun destinationReferencesJson(
        references: Map<FhirIdentifierKey, String>,
    ): JsonArray = buildJsonArray {
        references.toSortedMap().forEach { (identifier, reference) ->
            add(buildJsonObject {
                put("system", JsonPrimitive(identifier.system))
                put("value", JsonPrimitive(identifier.value))
                put("reference", JsonPrimitive(reference))
            })
        }
    }

    private fun parseDestinationReferences(value: JsonArray): Map<FhirIdentifierKey, String> =
        value.associate { element ->
            val item = element.jsonObject
            FhirIdentifierKey(item.string("system"), item.string("value")) to item.string("reference")
        }

    private fun targetJson(targets: Set<HealthConnectRetractionTarget>): JsonArray = buildJsonArray {
        targets.sortedWith(
            compareBy(
                { it.identifier.system },
                { it.identifier.value },
                { it.resourceType },
                { it.role.code },
            ),
        ).forEach { target ->
            add(buildJsonObject {
                put("system", JsonPrimitive(target.identifier.system))
                put("value", JsonPrimitive(target.identifier.value))
                put("identifierRole", JsonPrimitive(target.identifierRole.code))
                put("resourceType", JsonPrimitive(target.resourceType))
                put("role", JsonPrimitive(target.role.code))
            })
        }
    }

    private fun parseTargets(value: JsonArray): Set<HealthConnectRetractionTarget> = value.map { element ->
        val item = element.jsonObject
        HealthConnectRetractionTarget(
            identifier = FhirIdentifierKey(item.string("system"), item.string("value")),
            resourceType = item.string("resourceType"),
            role = HealthConnectRetractionTargetRole.entries.single { it.code == item.string("role") },
        ).also {
            check(it.identifierRole.code == item.string("identifierRole")) {
                "A stored retraction target names an Identifier role its target role cannot carry."
            }
        }
    }.toSet()
}

/** The exact framing the journal revision digest also encodes, so both stay one representation. */
internal fun identifierJson(identifier: Identifier): String =
    JsonParser().setOutputStyle(IParser.OutputStyle.NORMAL).composeString(
        Parameters().apply {
            addParameter().apply {
                name = "identifier"
                value = identifier.copy()
            }
        },
    )

private fun parseIdentifier(encoded: String): Identifier {
    val parameters = JsonParser().parse(encoded) as? Parameters
        ?: error("Stored source Identifier wrapper is not a Parameters resource.")
    return parameters.parameter.singleOrNull { it.name == "identifier" }?.value as? Identifier
        ?: error("Stored source Identifier wrapper does not contain exactly one Identifier.")
}

private fun parseBundle(encoded: String): Bundle = JsonParser().parse(encoded) as? Bundle
    ?: error("Stored journal payload is not a Bundle.")

private fun JsonObject.string(name: String): String =
    get(name)?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
        ?: error("Stored journal payload is missing $name.")

private fun JsonObject.nullableString(name: String): String? =
    get(name)?.takeUnless { it is JsonNull }?.jsonPrimitive?.content

private fun JsonObject.array(name: String): JsonArray = get(name) as? JsonArray
    ?: error("Stored journal payload is missing array $name.")

private fun JsonObject.objectValue(name: String): JsonObject = get(name) as? JsonObject
    ?: error("Stored journal payload is missing object $name.")
