//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.comorbidities

import edu.stanford.myheartcounts.model.demographics.Comorbidities

/**
 * Decides how choosing a condition or its date of diagnosis changes a participant's comorbidities,
 * and when having none of them can be reported.
 */
class ComorbiditiesRules {

    /**
     * [current] with [condition] chosen without a date of diagnosis, or unchosen.
     */
    fun toggled(current: Comorbidities?, condition: String): Comorbidities {
        val entries = current?.entries.orEmpty()
        return Comorbidities(
            entries = if (condition in entries) entries - condition else entries + (condition to ""),
        )
    }

    /**
     * [current] with [condition] diagnosed on [date]: `yyyy-MM`, `yyyy`, or empty when unknown.
     */
    fun diagnosed(
        current: Comorbidities?,
        condition: String,
        date: String,
    ): Comorbidities = Comorbidities(entries = current?.entries.orEmpty() + (condition to date))

    /**
     * Whether [current] reports having none of the conditions.
     */
    fun isNone(current: Comorbidities?): Boolean = current?.entries?.isEmpty() == true

    /**
     * Whether having none of the conditions is offered: only while no condition is chosen, so a stray
     * tap cannot clear every condition.
     */
    fun offersNone(current: Comorbidities?): Boolean = current?.entries.isNullOrEmpty()
}
