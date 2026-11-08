//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.comorbidities

/**
 * An interaction with the comorbidities screen.
 */
sealed interface ComorbiditiesAction {

    /**
     * The participant tapped the condition identified by [condition], choosing or unchoosing it.
     */
    data class ConditionSelected(val condition: String) : ComorbiditiesAction

    /**
     * The participant reported having none of the conditions.
     */
    data object NoneSelected : ComorbiditiesAction

    /**
     * The participant tapped the date of diagnosis of the chosen [condition].
     */
    data class DiagnosisDateSelected(val condition: String) : ComorbiditiesAction

    /**
     * The participant set the date of diagnosis of [condition] to [date]: `yyyy-MM`, `yyyy`, or empty
     * when unknown.
     */
    data class DiagnosisDateChanged(
        val condition: String,
        val date: String,
    ) : ComorbiditiesAction

    /**
     * The participant closed the date of diagnosis picker without a change.
     */
    data object DiagnosisDatePickerDismissed : ComorbiditiesAction

    /**
     * The participant left the screen.
     */
    data object BackSelected : ComorbiditiesAction
}
