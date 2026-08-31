//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import org.hl7.fhir.r4.formats.IParser
import org.hl7.fhir.r4.formats.JsonParser
import org.hl7.fhir.r4.model.Bundle
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant

/** Exact, transport-neutral encodings used by the acknowledged exchange boundary. */
object HealthConnectWireFormat {
    internal val MIN_FHIR_INSTANT: Instant = Instant.parse("0001-01-01T00:00:00Z")
    internal val MAX_FHIR_INSTANT: Instant = Instant.parse("9999-12-31T23:59:59.999999999Z")

    /**
     * Encodes Health Connect's nanosecond-precision source revision without numeric truncation.
     *
     * The result is a non-negative canonical decimal string containing epoch nanoseconds.
     */
    fun sourceVersion(value: Instant): String {
        requireFhirInstant(value, "Wire source version")
        require(!value.isBefore(Instant.EPOCH)) { "A wire source version must not precede the Unix epoch." }
        return BigInteger.valueOf(value.epochSecond)
            .multiply(NANOSECONDS_PER_SECOND)
            .add(BigInteger.valueOf(value.nano.toLong()))
            .toString()
    }

    /** Serializes one complete FHIR R4 Bundle to deterministic compact JSON for a caller-owned sink. */
    fun bundleJson(bundle: Bundle): String =
        JsonParser().setOutputStyle(IParser.OutputStyle.NORMAL).composeString(bundle)

    /** Rejects a Java Instant that cannot be serialized as a FHIR R4 instant/dateTime. */
    internal fun requireFhirInstant(value: Instant, field: String) {
        if (value < MIN_FHIR_INSTANT || value > MAX_FHIR_INSTANT) {
            throw InvalidHealthConnectRecord(
                "$field must have a four-digit FHIR year in the range 0001 through 9999.",
            )
        }
    }

    /** Returns lowercase SHA-256 for the exact UTF-8 string. */
    fun sha256(value: String): String =
        sha256(value.toByteArray(StandardCharsets.UTF_8))

    /** Returns lowercase SHA-256 for exact bytes such as a length-framed journal state. */
    internal fun sha256(value: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(value).let(::lowercaseHex)

    private fun lowercaseHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    private val NANOSECONDS_PER_SECOND = BigInteger.valueOf(NANOSECONDS_PER_SECOND_LONG)
    private const val NANOSECONDS_PER_SECOND_LONG = 1_000_000_000L
}
