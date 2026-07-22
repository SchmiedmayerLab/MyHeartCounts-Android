//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition.fixtures

import edu.stanford.spezi.studydefinition.CustomCriterionKey
import edu.stanford.spezi.studydefinition.EvaluationEnvironment

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
