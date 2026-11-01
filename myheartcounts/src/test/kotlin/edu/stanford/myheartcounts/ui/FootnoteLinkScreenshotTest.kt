//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui

import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.StringResource
import org.junit.Test

class FootnoteLinkScreenshotTest : ScreenshotTest() {

    @Test
    fun `FootnoteLink screenshot`() {
        val link = FootnoteLink(
            text = StringResource("Privacy Policy"),
            onClicked = {},
        )

        screenshot {
            link.Content()
        }
    }
}
