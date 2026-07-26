//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.model.demographics

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A participant's UK nation and county.
 */
@Serializable(with = UKRegion.Serializer::class)
sealed interface UKRegion {

    /**
     * A UK county, identified by its (non-localized) [name].
     */
    data class County(val name: String)

    /**
     * The selected county, or `null` for [NotSet].
     */
    val county: County?

    data object NotSet : UKRegion {
        override val county: County? get() = null
    }

    data class England(override val county: County) : UKRegion

    data class Scotland(override val county: County) : UKRegion

    data class Wales(override val county: County) : UKRegion

    data class NorthernIreland(override val county: County) : UKRegion

    object Serializer : KSerializer<UKRegion> {
        override val descriptor = PrimitiveSerialDescriptor("UKRegion", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: UKRegion) {
            val string = when (value) {
                NotSet -> NOT_SET
                is England -> "$ENGLAND$SEPARATOR${value.county.name}"
                is Scotland -> "$SCOTLAND$SEPARATOR${value.county.name}"
                is Wales -> "$WALES$SEPARATOR${value.county.name}"
                is NorthernIreland -> "$NORTHERN_IRELAND$SEPARATOR${value.county.name}"
            }
            encoder.encodeString(string)
        }

        override fun deserialize(decoder: Decoder): UKRegion {
            val string = decoder.decodeString()
            if (string == NOT_SET) return NotSet
            val separatorIndex = string.indexOf(SEPARATOR)
            if (separatorIndex < 0) return NotSet
            val county = County(string.substring(separatorIndex + 1))
            return when (string.substring(0, separatorIndex)) {
                ENGLAND -> England(county)
                SCOTLAND -> Scotland(county)
                WALES -> Wales(county)
                NORTHERN_IRELAND -> NorthernIreland(county)
                else -> NotSet
            }
        }

        private const val SEPARATOR = ":"
        private const val NOT_SET = "notSet"
        private const val ENGLAND = "england"
        private const val SCOTLAND = "scotland"
        private const val WALES = "wales"
        private const val NORTHERN_IRELAND = "northernIreland"
    }
}
