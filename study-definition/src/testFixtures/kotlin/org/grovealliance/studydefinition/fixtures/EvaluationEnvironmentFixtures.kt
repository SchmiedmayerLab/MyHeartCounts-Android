//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.studydefinition.fixtures

import org.grovealliance.studydefinition.CustomCriterionKey
import org.grovealliance.studydefinition.EvaluationEnvironment

/**
 * Fixture for [EvaluationEnvironment].
 */
object EvaluationEnvironmentFixtures {
    fun create(
        age: Int? = null,
        region: String? = null,
        language: String = "",
        enabledCustomKeys: Set<CustomCriterionKey> = emptySet(),
    ): EvaluationEnvironment = EvaluationEnvironment(
        age = age,
        region = region,
        language = language,
        enabledCustomKeys = enabledCustomKeys,
    )
}
