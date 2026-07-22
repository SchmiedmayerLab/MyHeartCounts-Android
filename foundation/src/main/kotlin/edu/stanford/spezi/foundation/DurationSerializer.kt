//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.foundation

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Duration

/**
 * A [KSerializer] for [Duration] that encodes the value as its whole-millisecond [Long].
 */
object DurationSerializer : KSerializer<Duration> {
    override val descriptor =
        PrimitiveSerialDescriptor("edu.stanford.spezi.foundation.Duration", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Duration) = encoder.encodeLong(value.toMillis())

    override fun deserialize(decoder: Decoder): Duration = Duration.ofMillis(decoder.decodeLong())
}
