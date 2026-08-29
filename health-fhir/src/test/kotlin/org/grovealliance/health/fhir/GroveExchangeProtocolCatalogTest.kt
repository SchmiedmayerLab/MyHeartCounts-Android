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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.hl7.fhir.r4.model.Identifier
import org.junit.Test
import java.io.File
import java.time.Instant

/** Executes the exact byte-level vectors published by catalog/exchange-protocol.json. */
class GroveExchangeProtocolCatalogTest {
    @Test
    fun `vendored vectors exactly match the configured normative catalog`() {
        val externalPath = System.getProperty(EXTERNAL_CATALOG_PROPERTY)?.takeIf(String::isNotBlank) ?: return
        val external = Json.parseToJsonElement(File(externalPath).readText()).jsonObject
            .getValue("testVectors")

        assertThat(external).isEqualTo(vectors)
    }

    @Test
    fun `closed HMAC kinds and arities exactly match the configured normative catalog`() {
        val externalPath = System.getProperty(EXTERNAL_CATALOG_PROPERTY)?.takeIf(String::isNotBlank) ?: return
        val catalogKinds = Json.parseToJsonElement(File(externalPath).readText()).jsonObject
            .getValue("opaqueIdentity").jsonObject
            .getValue("identityKinds").jsonArray
            .associate { element ->
                val kind = element.jsonObject
                kind.string("kind") to Pair(
                    kind.string("identifierRole"),
                    kind.array("components").size,
                )
            }
        val implementationKinds = GroveOpaqueIdentityKind.entries.associate { kind ->
            kind.code to (kind.identifierRole.code to kind.componentCount)
        }

        assertThat(implementationKinds).isEqualTo(catalogKinds)
    }

    @Test
    fun `matches every normative opaque identity vector`() {
        val key = GroveHmacIdentityKey.forConformanceTesting(
            TEST_OPAQUE_IDENTITY_SYSTEM_FAMILY,
            vectors.string("keyId"),
            vectors.string("epoch"),
            vectors.string("keyHex").hexBytes(),
        )

        val identities = vectors.array("identities")
        assertThat(identities.map { it.jsonObject.string("identityKind") }.toSet()).isEqualTo(
            GroveOpaqueIdentityKind.entries.map(GroveOpaqueIdentityKind::code).toSet(),
        )
        assertThat(identities).hasSize(GroveOpaqueIdentityKind.entries.size)
        identities.forEach { element ->
            val vector = element.jsonObject
            val kind = GroveOpaqueIdentityKind.entries.single {
                it.code == vector.string("identityKind")
            }
            val components = vector.array("components").map { it.jsonPrimitive.content }
            assertThat(key.value(kind, components)).isEqualTo(vector.string("value"))
        }
    }

    @Test
    fun `framing counts UTF-8 bytes and rejects ambiguous or malformed Unicode`() {
        assertThat(GroveExchangeProtocol.frameFields(listOf(""))).isEqualTo(
            byteArrayOf(0, 0, 0, 0),
        )
        assertThat(GroveExchangeProtocol.frameFields(listOf("😀"))).isEqualTo(
            byteArrayOf(0, 0, 0, 4, 0xf0.toByte(), 0x9f.toByte(), 0x98.toByte(), 0x80.toByte()),
        )
        assertThat(GroveExchangeProtocol.frameFields(listOf("a", "bc")))
            .isNotEqualTo(GroveExchangeProtocol.frameFields(listOf("ab", "c")))
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            GroveExchangeProtocol.frameFields(listOf("prefix\ud800suffix"))
        }
    }

    @Test
    fun `every closed HMAC kind rejects both missing and excess components`() {
        val key = GroveHmacIdentityKey.forConformanceTesting(
            TEST_OPAQUE_IDENTITY_SYSTEM_FAMILY,
            vectors.string("keyId"),
            vectors.string("epoch"),
            vectors.string("keyHex").hexBytes(),
        )

        GroveOpaqueIdentityKind.entries.forEach { kind ->
            val exact = validComponents(kind)
            assertThat(key.value(kind, exact)).matches("v0:test-key:1:[A-Za-z0-9_-]{43}")
            org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
                key.value(kind, exact.dropLast(1))
            }
            org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
                key.value(kind, exact + "excess")
            }
            org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
                key.value(kind, exact.toMutableList().apply { this[0] = "" })
            }
            org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
                key.value(kind, exact.toMutableList().apply { this[0] = "prefix\ud800suffix" })
            }
        }
    }

    @Test
    fun `provider coordinates require exact provider-specific identity domains`() {
        val key = GroveHmacIdentityKey.forConformanceTesting(
            TEST_OPAQUE_IDENTITY_SYSTEM_FAMILY,
            vectors.string("keyId"),
            vectors.string("epoch"),
            vectors.string("keyHex").hexBytes(),
        )
        val domainPairs = listOf(
            GroveOpaqueIdentityKind.SOURCE_RECORD to GroveOpaqueIdentityKind.PROVIDER_RECORD,
            GroveOpaqueIdentityKind.SOURCE_OUTPUT to GroveOpaqueIdentityKind.PROVIDER_OUTPUT,
            GroveOpaqueIdentityKind.SOURCE_ARTIFACT to GroveOpaqueIdentityKind.PROVIDER_ARTIFACT,
        )

        domainPairs.forEach { (genericKind, providerKind) ->
            val providerComponents = validComponents(providerKind)
            assertThat(key.value(providerKind, providerComponents))
                .matches("v0:test-key:1:[A-Za-z0-9_-]{43}")
            org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
                key.value(genericKind, providerComponents)
            }

            val genericComponents = validComponents(genericKind)
            assertThat(key.value(genericKind, genericComponents))
                .matches("v0:test-key:1:[A-Za-z0-9_-]{43}")
            org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
                key.value(providerKind, genericComponents)
            }
            org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
                key.value(
                    providerKind,
                    providerComponents.toMutableList().apply { this[0] = "invented-provider" },
                )
            }
        }
    }

    @Test
    fun `rejects every vendored invalid opaque identity vector`() {
        val invalidVectors = vectors.getValue("invalidIdentities").jsonArray
        assertThat(invalidVectors).hasSize(EXPECTED_INVALID_IDENTITY_VECTOR_COUNT)
        val key = GroveHmacIdentityKey.forConformanceTesting(
            TEST_OPAQUE_IDENTITY_SYSTEM_FAMILY,
            vectors.string("keyId"),
            vectors.string("epoch"),
            vectors.string("keyHex").hexBytes(),
        )

        invalidVectors.forEach { element ->
            val vector = element.jsonObject
            val kind = GroveOpaqueIdentityKind.entries.single {
                it.code == vector.string("identityKind")
            }
            val components = vector.array("components").map { it.jsonPrimitive.content }
            org.junit.Assert.assertThrows(vector.string("id"), IllegalArgumentException::class.java) {
                key.value(kind, components)
            }
        }
    }

    @Test
    fun `Health Connect sample builder reproduces the normative multi-output vector`() {
        val key = GroveHmacIdentityKey.forConformanceTesting(
            TEST_OPAQUE_IDENTITY_SYSTEM_FAMILY,
            vectors.string("keyId"),
            vectors.string("epoch"),
            vectors.string("keyHex").hexBytes(),
        )
        val vector = vectors.array("identities")
            .map { it.jsonObject }
            .single { it.string("id") == "multi-output-sample" }
        val source = HealthConnectIdentity.record(
            key,
            FhirIdentifierKey(
                vector.array("components")[2].jsonPrimitive.content,
                vector.array("components")[3].jsonPrimitive.content,
            ),
            vector.array("components")[1].jsonPrimitive.content,
            vector.array("components")[4].jsonPrimitive.content,
        )

        val output = HealthConnectIdentity.heartRateSampleOutput(
            key,
            source,
            Instant.parse("2026-08-19T10:30:00Z"),
            0,
        )

        assertThat(output.value).isEqualTo(vector.string("value"))
    }

    @Test
    fun `matches the normative event entry-node and fullUrl vectors`() {
        val eventVector = vectors.objectValue("event")
        val event = HealthConnectIdentity.exchange(
            eventVector.string("system"),
            eventVector.string("producerInstance"),
            EventSequence(eventVector.string("sequence")),
        )
        assertThat(event.value).isEqualTo(eventVector.string("value"))

        val nodeVector = vectors.objectValue("entryNode")
        val node = HealthConnectIdentity.conversionNode(nodeVector.string("system"), event)
        assertThat(node.value).isEqualTo(nodeVector.string("value"))
        assertThat(GroveExchangeIdentity.fullUrl(node)).isEqualTo(nodeVector.string("fullUrl"))

        vectors.array("fullUrls").forEach { element ->
            val vector = element.jsonObject
            val identifier = Identifier()
                .setSystem(vector.string("system"))
                .setValue(vector.string("value"))
            assertThat(GroveExchangeIdentity.fullUrl(identifier)).isEqualTo(vector.string("fullUrl"))
        }
    }

    private val vectors: JsonObject by lazy {
        val stream = requireNotNull(javaClass.getResourceAsStream(VENDORED_RESOURCE)) {
            "Missing vendored exchange-protocol test vectors."
        }
        stream.bufferedReader(Charsets.UTF_8).use { reader ->
            Json.parseToJsonElement(reader.readText()).jsonObject
        }
    }

    private fun JsonObject.string(name: String): String = getValue(name).jsonPrimitive.content

    private fun JsonObject.array(name: String): JsonArray = getValue(name).jsonArray

    private fun JsonObject.objectValue(name: String): JsonObject = getValue(name).jsonObject

    private fun validComponents(kind: GroveOpaqueIdentityKind): List<String> =
        List(kind.componentCount) { index ->
            when {
                index != 0 -> "component-$index"
                kind in PROVIDER_IDENTITY_KINDS -> "withings"
                kind in GENERIC_SOURCE_IDENTITY_KINDS -> "health-connect"
                else -> "component-0"
            }
        }

    private fun String.hexBytes(): ByteArray {
        require(length % 2 == 0 && all { it.digitToIntOrNull(16) != null }) {
            "A conformance key must be even-length hexadecimal."
        }
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    private companion object {
        const val VENDORED_RESOURCE = "/grove-exchange-protocol-test-vectors.json"
        const val EXTERNAL_CATALOG_PROPERTY = "grove.exchange-protocol.catalog"
        const val EXPECTED_INVALID_IDENTITY_VECTOR_COUNT = 4
        val PROVIDER_IDENTITY_KINDS = setOf(
            GroveOpaqueIdentityKind.PROVIDER_RECORD,
            GroveOpaqueIdentityKind.PROVIDER_OUTPUT,
            GroveOpaqueIdentityKind.PROVIDER_ARTIFACT,
        )
        val GENERIC_SOURCE_IDENTITY_KINDS = setOf(
            GroveOpaqueIdentityKind.SOURCE_RECORD,
            GroveOpaqueIdentityKind.SOURCE_OUTPUT,
            GroveOpaqueIdentityKind.SOURCE_ARTIFACT,
        )
    }
}
