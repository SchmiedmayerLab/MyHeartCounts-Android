//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.junit.Test

class GroveErrorLayoutScreenshotTest : ScreenshotTest() {

    @Test
    fun `GroveErrorLayout screenshot`() {
        val layout = GroveErrorLayout(
            image = ImageResource(Icons.Default.Warning),
            title = StringResource("Something went wrong!"),
            message = StringResource("An unexpected error occurred while loading the content. Please try again later."),
            primaryButton = AsyncTextButton(
                title = StringResource("Try again"),
                action = {}
            ),
            closeButton = GroveIconButton.close { },
        )

        screenshot { layout.Content() }
    }
}
