package edu.stanford.spezi.consent.internal

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import edu.stanford.spezi.markdown.MarkdownBlock
import edu.stanford.spezi.testing.screenshot.ScreenshotTest
import edu.stanford.spezi.ui.theme.Spacings
import org.junit.Test

@Suppress("MaxLineLength")
class ConsentMarkdownContentScreenshotTest : ScreenshotTest() {

    @Test
    fun `ConsentMarkdownContent nested lists screenshot`() {
        screenshot {
            ConsentMarkdownContent(
                block = MarkdownBlock.Markdown(
                    id = null,
                    rawContents = """
                        # Study Consent
                        Please review the consent details below before continuing.

                        ## Data sharing
                        - Health activity summaries
                            - Step count trends
                            - Walking distance estimates
                        - Survey responses
                            1. Eligibility questions
                            2. Follow-up questionnaires

                        ## Your choices
                        1. Read each section carefully
                            a. Ask questions if anything is unclear
                            b. Continue only when ready
                        2. Confirm your preferences

                        You can change your mind at any time.
                    """.trimIndent(),
                ),
            ).Content(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacings.medium)
            )
        }
    }
}
