//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.model.workout

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A participant's set of preferred workout types.
 */
@Serializable(with = WorkoutTypes.Serializer::class)
data class WorkoutTypes(val elements: Set<WorkoutType>) {

    object Serializer : KSerializer<WorkoutTypes> {
        override val descriptor = PrimitiveSerialDescriptor("WorkoutTypes", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: WorkoutTypes) =
            encoder.encodeString(value.elements.joinToString(SEPARATOR) { it.id })

        override fun deserialize(decoder: Decoder): WorkoutTypes {
            val ids = decoder.decodeString().split(SEPARATOR).filter { it.isNotBlank() }
            return WorkoutTypes(ids.mapNotNull { WorkoutType.fromId(it) }.toSet())
        }

        private const val SEPARATOR = ","
    }
}
