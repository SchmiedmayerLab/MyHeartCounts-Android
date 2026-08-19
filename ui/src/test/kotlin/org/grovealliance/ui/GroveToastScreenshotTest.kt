//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.junit.Test

class GroveToastScreenshotTest : ScreenshotTest() {

    @Test
    fun `GroveToast with icon and short message screenshot`() {
        val toast = GroveToast(
            image = ImageResource(Icons.Default.CheckCircle),
            message = StringResource("Changes saved successfully."),
            onClick = {},
        )

        screenshot { toast.Content() }
    }

    @Test
    fun `GroveToast with icon and long message screenshot`() {
        val toast = GroveToast(
            image = ImageResource(Icons.Default.Error),
            message = StringResource(
                "An unexpected error occurred while loading your data. Please check your connection and try again."
            ),
            onClick = {},
        )

        screenshot { toast.Content() }
    }

    @Test
    fun `GroveToast with long message and no icon screenshot`() {
        val toast = GroveToast(
            image = null,
            message = StringResource(
                "An unexpected error occurred while loading your data. Please check your connection and try again."
            ),
            onClick = {},
        )

        screenshot { toast.Content() }
    }
}
