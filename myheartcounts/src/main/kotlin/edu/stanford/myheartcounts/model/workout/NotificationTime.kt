//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("MagicNumber")

package edu.stanford.myheartcounts.model.workout

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A time of day for nudge notifications.
 */
@Serializable(with = NotificationTime.Serializer::class)
data class NotificationTime(val hour: Int, val minute: Int) {

    /**
     * The `"HH:mm"` representation.
     */
    val description: String get() = "%02d:%02d".format(hour, minute)

    object Serializer : KSerializer<NotificationTime> {
        override val descriptor = PrimitiveSerialDescriptor("NotificationTime", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: NotificationTime) = encoder.encodeString(value.description)
        override fun deserialize(decoder: Decoder): NotificationTime =
            parse(decoder.decodeString()) ?: NotificationTime(hour = 0, minute = 0)
    }

    companion object {
        /**
         * Parses a `"HH:mm"` string, or `null` if invalid.
         */
        fun parse(value: String): NotificationTime? {
            val parts = value.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it in 0..23 }
            val minute = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..59 }
            return if (hour != null && minute != null) NotificationTime(hour = hour, minute = minute) else null
        }
    }
}
