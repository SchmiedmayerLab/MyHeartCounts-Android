//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui

import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.StringResource
import org.junit.Test

class ArticleHeaderScreenshotTest : ScreenshotTest() {

    private val header = ArticleHeader(
        title = StringResource(text = "Welcome to My Heart Counts"),
        date = StringResource(text = "Jan 1, 2026"),
        tags = listOf(StringResource(text = "Study"), StringResource(text = "Heart Health")),
        image = ArticleHeader.HeaderImage.Stanford,
    )

    @Test
    fun `ArticleHeader with image and all metadata screenshot`() {
        screenshot(header = header)
    }

    @Test
    fun `ArticleHeader with title only screenshot`() {
        screenshot(header = header.copy(date = null, tags = emptyList(), image = null))
    }

    @Test
    fun `ArticleHeader with date only screenshot`() {
        screenshot(header = header.copy(tags = emptyList(), image = null))
    }

    @Test
    fun `ArticleHeader with tags only screenshot`() {
        screenshot(header = header.copy(date = null, image = null))
    }

    @Test
    fun `ArticleHeader with long title and overflowing tags screenshot`() {
        screenshot(
            header = header.copy(
                title = StringResource(text = "What comes next in your cardiovascular health study journey?"),
                tags = listOf(
                    StringResource(text = "Study Updates"),
                    StringResource(text = "Physical Activity"),
                    StringResource(text = "Heart Health"),
                ),
            ),
            width = 320.dp,
        )
    }

    @Test
    fun `ArticleHeader grows for large text and wrapping tags screenshot`() {
        screenshot(
            header = header.copy(
                title = StringResource(text = "What comes next in your cardiovascular health study journey?"),
                tags = listOf(
                    StringResource(text = "Study Updates"),
                    StringResource(text = "Physical Activity"),
                    StringResource(text = "Heart Health"),
                ),
            ),
            width = 320.dp,
            fontScale = 1.5f,
        )
    }

    private fun screenshot(header: ArticleHeader, width: Dp = 360.dp, fontScale: Float = 1f) {
        screenshot {
            CompositionLocalProvider(
                LocalDensity provides Density(density = LocalDensity.current.density, fontScale = fontScale),
            ) {
                Surface(modifier = Modifier.width(width)) { header.Content() }
            }
        }
    }
}
