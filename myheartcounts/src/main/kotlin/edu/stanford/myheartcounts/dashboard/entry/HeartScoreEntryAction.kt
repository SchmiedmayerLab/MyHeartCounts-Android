//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.entry

import java.time.LocalDateTime

/**
 * Changes requested while entering a measurement.
 */
sealed interface HeartScoreEntryAction {
    data class FieldChanged(val field: Field, val value: String) : HeartScoreEntryAction
    data class DateChanged(val value: LocalDateTime) : HeartScoreEntryAction
    data class ModeChanged(val direct: Boolean) : HeartScoreEntryAction
    data object Save : HeartScoreEntryAction
    data object Close : HeartScoreEntryAction

    enum class Field { Value, Diastolic, Weight, Height, Inches }
}
