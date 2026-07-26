//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("MagicNumber")

package edu.stanford.myheartcounts.model.demographics

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

/**
 * A participant's blood type.
 */
@Serializable(with = BloodType.Serializer::class)
enum class BloodType(val rawValue: Int) {
    NOT_SET(0),
    A_POSITIVE(1),
    A_NEGATIVE(2),
    B_POSITIVE(3),
    B_NEGATIVE(4),
    AB_POSITIVE(5),
    AB_NEGATIVE(6),
    O_POSITIVE(7),
    O_NEGATIVE(8),
    ;

    object Serializer : KSerializer<BloodType> by IntRawValueSerializer(
        serialName = "BloodType",
        entries = entries,
        rawValue = { it.rawValue },
        default = NOT_SET,
    )
}
