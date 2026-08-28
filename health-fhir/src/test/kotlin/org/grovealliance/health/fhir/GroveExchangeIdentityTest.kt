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
    fun `fullUrl is deterministic and its entry extension retains the exact identifier`() {
        val identifier = Identifier()
            .setSystem("https://xn--fsq.example/%E8%AD%98%E5%88%A5%E5%AD%90")
            .setValue("café|東京")

        assertThat(GroveExchangeIdentity.fullUrl(identifier))
            .isEqualTo("urn:uuid:d35e4203-71f6-595c-bd1b-306b8414974e")
        assertThat(GroveExchangeIdentity.fullUrl(identifier)).isEqualTo(GroveExchangeIdentity.fullUrl(identifier))
        assertThat(
            (GroveExchangeIdentity.entryIdentifierExtension(identifier).value as Identifier).equalsDeep(identifier),
        ).isTrue()
    }

    @Test
    fun `framing keeps separators and valid supplementary scalars unambiguous`() {
        val withSeparator = Identifier().setSystem("https://example.org/system").setValue("a|b")
        val withEmoji = Identifier().setSystem("https://example.org/system").setValue("a😀b")

        assertThat(GroveExchangeIdentity.fullUrl(withSeparator))
            .isNotEqualTo(GroveExchangeIdentity.fullUrl(withEmoji))
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
            GroveExchangeIdentity.fullUrl(Identifier().setSystem("not-absolute").setValue("value"))
        }
        listOf(
            "https://例.example/識別子",
            "https://example.org/has space",
            "https://example.org/%ZZ",
        ).forEach { invalidSystem ->
            assertThrows(IllegalArgumentException::class.java) {
                GroveExchangeIdentity.fullUrl(
                    Identifier().setSystem(invalidSystem).setValue("value"),
                )
            }
        }
    }
}
