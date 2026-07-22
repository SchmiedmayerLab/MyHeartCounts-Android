//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.scheduler

/**
 * User-visible category information of a [Task].
 *
 * UI layers use the category to communicate the kind of task to the user.
 */
@JvmInline
value class TaskCategory(val rawValue: String) {
    companion object {
        /**
         * A task that prompts the user to answer a questionnaire.
         */
        val questionnaire = TaskCategory("questionnaire")

        /**
         * A task that prompts the user to record a measurement.
         */
        val measurement = TaskCategory("measurement")

        /**
         * A task related to medication.
         */
        val medication = TaskCategory("medication")

        /**
         * A category identified by a custom [label].
         */
        fun custom(label: String) = TaskCategory(label)
    }
}
