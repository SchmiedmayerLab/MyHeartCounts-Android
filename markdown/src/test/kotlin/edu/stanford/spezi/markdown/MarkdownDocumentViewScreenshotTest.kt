package edu.stanford.spezi.markdown

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import edu.stanford.spezi.testing.screenshot.ScreenshotTest
import edu.stanford.spezi.ui.SpeziCard
import edu.stanford.spezi.ui.theme.Spacings
import edu.stanford.spezi.ui.theme.TextStyles
import edu.stanford.spezi.ui.theme.bold
import org.junit.Test

@Suppress("MaxLineLength")
class MarkdownDocumentViewScreenshotTest : ScreenshotTest() {

    @Test
    fun `MarkdownDocumentView rich document screenshot`() {
        screenshot {
            MarkdownDocumentView(
                modifier = Modifier.fillMaxSize(),
                document = MarkdownDocument.process(
                    """
                    ---
                    title: Study Information
                    version: 1.0.0
                    ---
                    # Welcome
                    This paragraph includes **bold emphasis**, *italic emphasis*, and a website: https://example.org.

                    ## What to expect
                    Review each section carefully before continuing.
                    - A bullet item with www.example.org/details
                        - A nested bullet item
                            - A deeply nested bullet item
                    * Another bullet item with support@example.org
                    + A final bullet item with tel:+123456789

                    ### Ordered steps
                    1. Read the study information
                        a. Review the nested detail
                    2. Ask questions if anything is unclear
                    a. Confirm your choices
                    b. Continue when ready

                    #### Privacy note
                    Your participation is voluntary and you can stop sharing data at any time. The study team only uses
                    the information you choose to provide for approved research activities. If you change your mind,
                    contact the study team or update your preferences in the app.

                    ##### Contact
                    Email support@example.org with questions about the document.
                    """.trimIndent()
                ),
            )
        }
    }

    @Test
    fun `MarkdownDocumentView custom element screenshot`() {
        screenshot {
            MarkdownDocumentView(
                modifier = Modifier.fillMaxSize(),
                document = MarkdownDocument.process(
                    text = """
                        ---
                        title: Study Update
                        ---
                        # Before you continue
                        Please review the notice below before moving to the next section.

                        <notice id="data-sharing" title="Data sharing reminder">
                        You can **pause sharing** at any time. Email support@example.org if you need help changing your preferences.
                        </>

                        ## Next steps
                        - Review your current preferences
                        - Continue when everything looks correct
                    """.trimIndent(),
                    customElementNames = setOf("notice"),
                ),
                elementContent = { element ->
                    NoticeElement(element = element)
                },
            )
        }
    }

    @Composable
    private fun NoticeElement(element: MarkdownBlock.Element) {
        SpeziCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(Spacings.medium),
                verticalArrangement = Arrangement.spacedBy(Spacings.small),
            ) {
                element.attribute("title")?.let { title ->
                    Text(
                        text = title,
                        style = TextStyles.titleMedium.bold(),
                    )
                }
                val text = element.content.filterIsInstance<MarkdownBlock.Element.Content.Text>()
                    .joinToString(separator = "\n\n") { it.text }
                MarkdownTextBlock(text = text)
            }
        }
    }
}
