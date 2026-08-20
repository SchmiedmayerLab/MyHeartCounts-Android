//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import org.grovealliance.markdown.MarkdownDocument
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.groveAppBar
import org.junit.Test

class MarkdownSheetScreenshotTest : ScreenshotTest() {

    @Test
    fun `MarkdownSheet screenshot`() {
        screenshot {
            MarkdownSheet(
                appBar = groveAppBar {
                    close { }
                },
                document = article,
            ).Sheet(modifier = Modifier.fillMaxSize())
        }
    }

    private val article = MarkdownDocument.process(
        text = """
        ---
        title: Welcome to My Heart Counts
        lede: Learn about the study
        ---
        # Welcome to the My Heart Counts Study!
        We're thrilled to have you on board. Your contributions are instrumental in advancing our understanding of cardiovascular health.

        ## Surveys
        - **Frequency:** We encourage you to complete the surveys daily.
        - **Flexibility:** A survey you miss stays available for later completion.
        """.trimIndent()
    )
}
