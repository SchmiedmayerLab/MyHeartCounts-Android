//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("MagicNumber")

package edu.stanford.myheartcounts.model.demographics

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

/**
 * A participant's household income bracket (US).
 */
@Serializable(with = HouseholdIncomeUS.Serializer::class)
enum class HouseholdIncomeUS(val rawValue: Int) {
    NOT_SET(0),
    LESS_THAN_15K(1),
    FROM_15K_TO_24999(2),
    FROM_25K_TO_34999(3),
    FROM_35K_TO_49999(4),
    FROM_50K_TO_74999(5),
    FROM_75K_TO_99999(6),
    FROM_100K_TO_149999(7),
    ABOVE_150K(8),
    PREFER_NOT_TO_STATE(255),
    ;

    object Serializer : KSerializer<HouseholdIncomeUS> by IntRawValueSerializer(
        serialName = "HouseholdIncomeUS",
        entries = entries,
        rawValue = { it.rawValue },
        default = NOT_SET,
    )
}

/**
 * A participant's household income bracket (UK).
 */
@Serializable(with = HouseholdIncomeUK.Serializer::class)
enum class HouseholdIncomeUK(val rawValue: Int) {
    NOT_SET(0),
    LESS_THAN_15K(1),
    FROM_15K_TO_24999(2),
    FROM_25K_TO_34999(3),
    FROM_35K_TO_49999(4),
    FROM_50K_TO_74999(5),
    FROM_75K_TO_99999(6),
    FROM_100K_TO_149999(7),
    ABOVE_150K(8),
    PREFER_NOT_TO_STATE(255),
    ;

    object Serializer : KSerializer<HouseholdIncomeUK> by IntRawValueSerializer(
        serialName = "HouseholdIncomeUK",
        entries = entries,
        rawValue = { it.rawValue },
        default = NOT_SET,
    )
}
