//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SuspendButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun init() {
        composeTestRule.setContent {
            SuspendButtonTestComposable()
        }
    }

    @Test
    fun testSuspendButton() {
        suspendButton {
            clickHelloWorldButton()
            waitForHelloWorldButtonAction()
            resetHelloWorldButtonAction()

            clickHelloThrowingWorldButton()
            assertViewStateAlertAppeared("Error was thrown!")
            dismissViewStateAlert()
            assertHelloThrowingWorldButtonIsEnabled()
        }
    }

    private fun suspendButton(block: SuspendButtonTestSimulator.() -> Unit) {
        SuspendButtonTestSimulator(composeTestRule).apply(block)
    }
}
