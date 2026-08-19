//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.studydefinition.fixtures

import org.grovealliance.studydefinition.CustomCriterionKey

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
