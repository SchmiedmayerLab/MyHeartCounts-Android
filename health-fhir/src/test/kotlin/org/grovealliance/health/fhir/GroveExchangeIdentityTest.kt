//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import com.google.common.truth.Truth.assertThat
import org.hl7.fhir.r4.model.Identifier
import org.junit.Assert.assertThrows
import org.junit.Test

class GroveExchangeIdentityTest {
    @Test
    fun `matches every normative exchange identity vector`() {
        listOf(
            Vector(
                "https://study.example.org/fhir/identifiers/mobile-observation",
                "heart-rate-20260820-001",
                "https://study.example.org/fhir/identifiers/mobile-observation|heart-rate-20260820-001",
                "urn:uuid:72a64652-0bad-517d-8a36-39e3b6adccac",
            ),
            Vector(
                "https://\u4f8b.example/\u8b58\u5225\u5b50",
                "caf\u00e9-\u6771\u4eac",
                "https://\u4f8b.example/\u8b58\u5225\u5b50|caf\u00e9-\u6771\u4eac",
                "urn:uuid:31acad95-5e9a-5b0f-b5b7-4f4627825b6b",
            ),
            // A composed identifier value carries vertical bars of its own; only the system may not.
            Vector(
                "https://grovealliance.org/fhir/mobile/NamingSystem/grove-writer-record-id",
                "v1:com.withings.wiscale2|17348211",
                "https://grovealliance.org/fhir/mobile/NamingSystem/grove-writer-record-id" +
                    "|v1:com.withings.wiscale2|17348211",
                "urn:uuid:68db0fd4-0146-59c9-86cf-934f00881095",
            ),
        ).forEach { vector ->
            val identifier = Identifier().setSystem(vector.system).setValue(vector.value)
            assertThat(GroveExchangeIdentity.identifierName(vector.system, vector.value))
                .isEqualTo(vector.input)
            assertThat(GroveExchangeIdentity.fullUrl(identifier)).isEqualTo(vector.fullUrl)
            assertThat(
                (GroveExchangeIdentity.entryIdentifierExtension(identifier).value as Identifier).equalsDeep(identifier),
            )
                .isTrue()
        }
    }

    @Test
    fun `rejects incomplete identifiers and isolated surrogates`() {
        assertThrows(IllegalArgumentException::class.java) {
            GroveExchangeIdentity.fullUrl(Identifier().setSystem("https://example.org/system"))
        }
        listOf("\ud800", "\udc00", "prefix\ud800suffix").forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) {
                GroveExchangeIdentity.identifierName("https://example.org/system", invalid)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            GroveExchangeIdentity.identifierName("https://example.org/a|b", "value")
        }
    }

    private data class Vector(
        val system: String,
        val value: String,
        val input: String,
        val fullUrl: String,
    )
}
