//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.junit.Test

class GroveAppBarScreenshotTests : ScreenshotTest() {

    @Test
    fun `GroveAppBar text title only screenshot`() {
        val appBar = groveAppBar {
            title("Hello Grove")
        }
        screenshot { appBar.Content() }
    }

    @Test
    fun `GroveAppBar complete screenshot`() {
        val appBar = groveAppBar {
            title("Hello Grove")
            close { }
            action(ImageResource(Icons.Default.Favorite))
        }
        screenshot { appBar.Content() }
    }

    @Test
    fun `GroveAppBar back only screenshot`() {
        val appBar = groveAppBar {
            back { }
        }
        screenshot { appBar.Content() }
    }

    @Test
    fun `GroveAppBar composable navigation screenshot`() {
        val appBar = groveAppBar {
            navigation(ImageResource(Icons.Default.MoreVert))
        }
        screenshot { appBar.Content() }
    }

    @Test
    fun `GroveAppBar widget navigation screenshot`() {
        val appBar = groveAppBar {
            navigation(GroveAppBarWidgets.navigation(ImageResource(Icons.Default.Close)) { })
        }
        screenshot { appBar.Content() }
    }

    @Test
    fun `GroveAppBar non center aligned screenshot`() {
        val appBar = groveAppBar {
            title("Hello Grove")
            centerAlign(false)
        }
        screenshot { appBar.Content() }
    }
}
