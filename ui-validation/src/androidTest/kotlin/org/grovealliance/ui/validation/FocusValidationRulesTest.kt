//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.validation

import androidx.compose.ui.test.junit4.createComposeRule
import org.grovealliance.ui.validation.composables.FocusValidationRules
import org.grovealliance.ui.validation.simulators.FocusValidationRulesSimulator
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FocusValidationRulesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun init() {
        composeTestRule.setContent {
            FocusValidationRules()
        }
    }

    @Test
    fun testFocusValidationRules() {
        focusValidationRules {
            assertHasEngines(true)
            assertInputValid(false)
            assertPasswordMessageExists(false)
            assertEmptyMessageExists(false)
            clickValidateButton()
            assertLastState(false)
            assertPasswordMessageExists(true)
            assertEmptyMessageExists(true)
            enterEmail("leland@stanford.edu")
            assertEmptyMessageExists(false)
            assertPasswordMessageExists(true)
            enterPassword("password")
            assertEmptyMessageExists(false)
            assertPasswordMessageExists(false)
        }
    }

    private fun focusValidationRules(block: FocusValidationRulesSimulator.() -> Unit) {
        FocusValidationRulesSimulator(composeTestRule).apply(block)
    }
}
