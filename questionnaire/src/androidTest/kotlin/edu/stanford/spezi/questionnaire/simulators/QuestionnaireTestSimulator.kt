//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.questionnaire.simulators

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import edu.stanford.spezi.questionnaire.QuestionnaireComposableTestIdentifiers
import edu.stanford.spezi.testing.ui.onNodeWithIdentifier

class QuestionnaireTestSimulator(
    val composeTestRule: ComposeTestRule,
) {
    private val root =
        composeTestRule.onNodeWithIdentifier(QuestionnaireComposableTestIdentifiers.ROOT)

    fun assertIsDisplayed() {
        root.assertIsDisplayed()
    }
}
