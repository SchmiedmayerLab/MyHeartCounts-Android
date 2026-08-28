//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.hl7.fhir.r4.formats.JsonParser
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Provenance
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.time.Instant

/** Checks this producer boundary against the shared positive active and retraction corpus. */
class GroveMobileExchangeCorpusTest {
    @Test
    fun `shared active and retraction examples satisfy the Kotlin event boundary`() {
        val directory = System.getProperty(CORPUS_DIRECTORY_PROPERTY)
            ?.takeIf(String::isNotBlank)
            ?.let(::File)
            ?: return
        val manifest = Json.parseToJsonElement(directory.resolve("corpus.json").readText()).jsonObject
        assertThat(manifest.getValue("version").jsonPrimitive.content)
            .isEqualTo(HealthConnectContract.PACKAGE_VERSION)

        val active = parseBundle(directory.resolve("exchange-bundle.json"))
        val activeSource = active.entry.mapNotNull { it.resource as? Observation }.single()
            .identifier.single { it.hasGroveRole(GroveIdentifierRole.SOURCE_RECORD) }
        eventBatch(
            operation = HealthConnectExportOperation.ACTIVE,
            bundle = active,
            source = activeSource,
            targets = emptySet(),
        )

        val retraction = parseBundle(directory.resolve("retraction-bundle.json"))
        val lifecycle = retraction.entry.map { it.resource }.filterIsInstance<Provenance>().single()
        val retractionSource = lifecycle.entity.single().what.identifier
        val targets = lifecycle.target.map { target ->
            val identifierRole = target.identifier.type.coding.single {
                it.system == HealthConnectContract.GROVE_IDENTIFIER_ROLE
            }.code
            val targetRole = (target.extension.single {
                it.url == HealthConnectContract.GROVE_RETRACTION_TARGET_ROLE
            }.value as CodeType).value
            HealthConnectRetractionTarget(
                identifier = target.identifier.key(),
                identifierRole = GroveIdentifierRole.entries.single { it.code == identifierRole },
                resourceType = target.type,
                role = HealthConnectRetractionTargetRole.entries.single { it.code == targetRole },
            )
        }.toSet()
        eventBatch(
            operation = HealthConnectExportOperation.RETRACTION,
            bundle = retraction,
            source = retractionSource,
            targets = targets,
        )
    }

    @Test
    fun `every exact structured mutation fails the Kotlin event boundary`() {
        val directory = System.getProperty(CORPUS_DIRECTORY_PROPERTY)
            ?.takeIf(String::isNotBlank)
            ?.let(::File)
            ?: return
        val manifest = Json.parseToJsonElement(directory.resolve("corpus.json").readText()).jsonObject
        val cases = manifest.getValue("cases").jsonArray.map { it.jsonObject }
        assertThat(cases).hasSize(EXPECTED_MUTATION_COUNT)
        assertThat(cases.map { it.getValue("id").jsonPrimitive.content })
            .containsExactlyElementsIn(EXPECTED_MUTATION_IDS)

        val bases = manifest.getValue("bases").jsonArray.associate { base ->
            val value = base.jsonObject
            value.getValue("id").jsonPrimitive.content to value.getValue("path").jsonPrimitive.content
        }
        cases.forEach { case ->
            val baseId = case.getValue("base").jsonPrimitive.content
            val baseElement = Json.parseToJsonElement(directory.resolve(bases.getValue(baseId)).readText())
            val baseBundle = JsonParser().parse(baseElement.toString()) as Bundle
            val operation = if (baseId == "mobile-retraction") {
                HealthConnectExportOperation.RETRACTION
            } else {
                HealthConnectExportOperation.ACTIVE
            }
            val source = sourceIdentifier(baseBundle, operation)
            val targets = if (operation == HealthConnectExportOperation.RETRACTION) {
                retractionTargets(baseBundle)
            } else {
                emptySet()
            }
            val mutated = case.getValue("patch").jsonArray.fold(baseElement) { document, operationPatch ->
                document.applyPatch(operationPatch.jsonObject)
            }

            assertThrows(Exception::class.java) {
                val bundle = JsonParser().parse(mutated.toString()) as Bundle
                eventBatch(operation, bundle, source, targets)
            }
        }
    }

    private fun sourceIdentifier(
        bundle: Bundle,
        operation: HealthConnectExportOperation,
    ): Identifier = when (operation) {
        HealthConnectExportOperation.ACTIVE -> bundle.entry
            .mapNotNull { it.resource as? Observation }
            .first()
            .identifier
            .single { it.hasGroveRole(GroveIdentifierRole.SOURCE_RECORD) }
            .copy()
        HealthConnectExportOperation.RETRACTION -> bundle.entry
            .map { it.resource }
            .filterIsInstance<Provenance>()
            .single()
            .entity
            .single()
            .what
            .identifier
            .copy()
    }

    private fun retractionTargets(bundle: Bundle): Set<HealthConnectRetractionTarget> =
        bundle.entry.map { it.resource }.filterIsInstance<Provenance>().single().target.map { target ->
            val identifierRole = target.identifier.type.coding.single {
                it.system == HealthConnectContract.GROVE_IDENTIFIER_ROLE
            }.code
            val targetRole = (target.extension.single {
                it.url == HealthConnectContract.GROVE_RETRACTION_TARGET_ROLE
            }.value as CodeType).value
            HealthConnectRetractionTarget(
                identifier = target.identifier.key(),
                identifierRole = GroveIdentifierRole.entries.single { it.code == identifierRole },
                resourceType = target.type,
                role = HealthConnectRetractionTargetRole.entries.single { it.code == targetRole },
            )
        }.toSet()

    private fun JsonElement.applyPatch(patch: JsonObject): JsonElement {
        val operation = patch.getValue("op").jsonPrimitive.content
        val tokens = patch.getValue("path").jsonPrimitive.content
            .split('/')
            .drop(1)
            .map { it.replace("~1", "/").replace("~0", "~") }
        return patched(tokens, operation, patch["value"])
    }

    private fun JsonElement.patched(
        tokens: List<String>,
        operation: String,
        value: JsonElement?,
    ): JsonElement {
        require(tokens.isNotEmpty()) { "The shared corpus does not patch the JSON document root." }
        val head = tokens.first()
        val tail = tokens.drop(1)
        return when (this) {
            is JsonObject -> patchedObject(head, tail, operation, value)
            is JsonArray -> patchedArray(head, tail, operation, value)
            else -> error("Corpus JSON Pointer traverses a primitive at /${tokens.joinToString("/")}.")
        }
    }

    private fun JsonObject.patchedObject(
        key: String,
        remaining: List<String>,
        operation: String,
        value: JsonElement?,
    ): JsonObject = JsonObject(toMutableMap().apply {
        if (remaining.isNotEmpty()) {
            put(key, getValue(key).patched(remaining, operation, value))
            return@apply
        }
        when (operation) {
            "add", "replace" -> put(key, requireNotNull(value))
            "remove" -> requireNotNull(remove(key))
            else -> error("Unsupported corpus patch operation $operation")
        }
    })

    private fun JsonArray.patchedArray(
        token: String,
        remaining: List<String>,
        operation: String,
        value: JsonElement?,
    ): JsonArray = JsonArray(toMutableList().apply {
        if (remaining.isNotEmpty()) {
            val index = token.toInt()
            set(index, get(index).patched(remaining, operation, value))
            return@apply
        }
        when (operation) {
            "add" -> if (token == "-") {
                add(requireNotNull(value))
            } else {
                add(token.toInt(), requireNotNull(value))
            }
            "replace" -> set(token.toInt(), requireNotNull(value))
            "remove" -> removeAt(token.toInt())
            else -> error("Unsupported corpus patch operation $operation")
        }
    })

    private fun eventBatch(
        operation: HealthConnectExportOperation,
        bundle: Bundle,
        source: org.hl7.fhir.r4.model.Identifier,
        targets: Set<HealthConnectRetractionTarget>,
    ): HealthConnectExportBatch {
        val bundleJson = HealthConnectWireFormat.bundleJson(bundle)
        return HealthConnectExportBatch(
            eventSequence = EventSequence(bundle.identifier.value.substringAfterLast(':')),
            operation = operation,
            sourceRecordIdentifier = source,
            sourceVersion = Instant.parse(bundle.timestampElement.valueAsString),
            bundle = bundle,
            bundleJson = bundleJson,
            payloadSha256 = HealthConnectWireFormat.sha256(bundleJson),
            retractedTargets = targets,
        )
    }

    private fun parseBundle(file: File): Bundle = JsonParser().parse(file.readText()) as Bundle

    private companion object {
        const val CORPUS_DIRECTORY_PROPERTY = "grove.mobile-exchange.corpus-directory"
        const val EXPECTED_MUTATION_COUNT = 31
        val EXPECTED_MUTATION_IDS = setOf(
            "missing-entry-node-key",
            "non-deterministic-full-url",
            "unresolved-internal-reference",
            "wrong-heart-rate-unit",
            "non-canonical-event-identity",
            "tampered-entry-node-digest",
            "missing-source-output-identity",
            "identity-system-changes-role",
            "missing-transform-provenance",
            "retraction-literal-target",
            "retraction-unknown-target-role",
            "retraction-clear-target-identity",
            "retraction-copied-clinical-resource",
            "ambiguous-active-lifecycle-coding",
            "contradictory-retraction-lifecycle-coding",
            "unprofiled-active-observation",
            "wrong-subject-target-type",
            "false-reference-declared-type",
            "transform-literal-source-entity",
            "retraction-role-target-type-mismatch",
            "retraction-additional-source-entity",
            "mixed-literal-logical-patient-reference",
            "untyped-logical-patient-reference",
            "unadmitted-condition-resource",
            "adapter-only-output-without-adapter-profile",
            "contained-resource-prohibited",
            "unadmitted-device-metric-resource",
            "unprofiled-active-document-reference",
            "unprofiled-active-device",
            "unprofiled-active-provenance",
            "disconnected-supporting-patient",
        )
    }
}
