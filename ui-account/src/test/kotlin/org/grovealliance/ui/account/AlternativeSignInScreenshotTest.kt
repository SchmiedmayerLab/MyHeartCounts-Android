//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.account

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import org.grovealliance.resources.Drawables
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.junit.Test

class AlternativeSignInScreenshotTest : ScreenshotTest() {

    @Test
    fun `AlternativeSignIn screenshot`() {
        val content = AlternativeSignIn(
            divider = LabeledHorizontalDivider(
                label = StringResource("or"),
            ),
            buttons = listOf(
                AsyncTextButton(
                    title = StringResource("Sign in with Google"),
                    icon = ImageResource(Drawables.ic_google),
                    action = {}
                ),
                AsyncTextButton(
                    title = StringResource("Sign in with Stanford"),
                    icon = ImageResource(Icons.Default.School),
                    action = {}
                )
            )
        )
        screenshot {
            content.Content()
        }
    }
}
