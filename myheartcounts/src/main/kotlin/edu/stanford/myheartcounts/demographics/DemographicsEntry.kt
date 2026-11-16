//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import edu.stanford.myheartcounts.demographics.ui.DemographicsEntrySheet

/**
 * A question answered by typing in a value on a [DemographicsEntrySheet].
 */
enum class DemographicsEntry {
    /**
     * Height, in the participant's units.
     */
    Height,

    /**
     * Weight, in the participant's units.
     */
    Weight,

    /**
     * The NHS number; an empty entry clears it.
     */
    NHSNumber,
}
