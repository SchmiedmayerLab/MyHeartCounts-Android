//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.entry

import org.grovealliance.ui.StringResource

/**
 * The editable measurement and its save state.
 */
data class HeartScoreEntryState(
    val form: HeartScoreEntryForm,
    val canSave: Boolean,
    val bmi: Double?,
    val saving: Boolean = false,
    val error: StringResource? = null,
)
