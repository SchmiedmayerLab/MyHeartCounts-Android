//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.entry

import org.grovealliance.ui.StringResource

/**
 * The editable measurement, its save state and the date or time picker currently open.
 */
data class HeartScoreEntryState(
    val form: HeartScoreEntryForm,
    val canSave: Boolean,
    val bmi: Double?,
    val saving: Boolean = false,
    val error: StringResource? = null,
    val picker: Picker? = null,
) {

    /**
     * A picker for the measurement's date.
     */
    enum class Picker {
        Date,
        Time,
    }
}
