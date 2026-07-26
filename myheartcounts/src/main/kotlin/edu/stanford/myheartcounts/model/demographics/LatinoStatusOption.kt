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
 * A participant's Latino / Hispanic status.
 */
@Serializable(with = LatinoStatusOption.Serializer::class)
enum class LatinoStatusOption(val rawValue: Int) {
    NOT_SET(0),
    NO(1),
    YES_MEXICAN(2),
    YES_CARIBBEAN(3),
    YES_SOUTH_AMERICAN(4),
    YES_EUROPEAN(5),
    YES_OTHER(6),
    PREFER_NOT_TO_STATE(255),
    ;

    object Serializer : KSerializer<LatinoStatusOption> by IntRawValueSerializer(
        serialName = "LatinoStatusOption",
        entries = entries,
        rawValue = { it.rawValue },
        default = NOT_SET,
    )
}
