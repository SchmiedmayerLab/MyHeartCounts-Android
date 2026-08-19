//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.questionnaire

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.grovealliance.questionnaire.composables.QuestionnaireTestComposable
import org.grovealliance.questionnaire.simulators.QuestionnaireTestSimulator
import org.grovealliance.testing.ui.ComposeContentActivity
import org.junit.Rule
import org.junit.Test

class QuestionnaireTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComposeContentActivity>()

    @Test
    fun testQuestionnaireComposableDisplay() {
        composeTestRule.activity.setScreen { QuestionnaireTestComposable() }
        questionnaireComposable {
            assertIsDisplayed()
        }
    }

    private fun questionnaireComposable(block: QuestionnaireTestSimulator.() -> Unit) {
        QuestionnaireTestSimulator(composeTestRule).apply { block() }
    }
}
