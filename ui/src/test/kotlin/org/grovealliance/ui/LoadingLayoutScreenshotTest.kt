//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.junit.Test

class LoadingLayoutScreenshotTest : ScreenshotTest() {

    @Test
    fun `LoadingLayout with message screenshot`() {
        val layout = LoadingLayout(
            message = StringResource("Loading data, please wait...")
        )

        screenshot { layout.Content(modifier = Modifier.fillMaxSize()) }
    }

    @Test
    fun `LoadingLayout without message screenshot`() {
        val layout = LoadingLayout()

        screenshot { layout.Content(modifier = Modifier.fillMaxSize()) }
    }
}
