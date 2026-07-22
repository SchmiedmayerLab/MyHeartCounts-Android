//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition.fixtures

import edu.stanford.spezi.studydefinition.CustomCriterionKey

/**
 * Fixture for [CustomCriterionKey].
 */
object CustomCriterionKeyFixtures {
    fun create(
        keyValue: String = "",
        displayTitle: String = "",
    ): CustomCriterionKey = CustomCriterionKey(
        keyValue = keyValue,
        displayTitle = displayTitle,
    )
}
