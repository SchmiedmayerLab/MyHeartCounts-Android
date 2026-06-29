package edu.stanford.spezi.markdown

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import edu.stanford.spezi.ui.theme.Spacings
import edu.stanford.spezi.ui.theme.SpeziTheme
import edu.stanford.spezi.ui.theme.TextStyles
import edu.stanford.spezi.ui.theme.ThemePreviews
import edu.stanford.spezi.ui.theme.bold

/**
 * Renders a [MarkdownDocument] as a vertically scrolling list of blocks.
 *
 * When the document's metadata contains a [MarkdownMetadata.title], it is rendered above the block
 * list. Custom HTML-style elements are delegated to [elementContent]; unhandled elements produce no
 * output.
 */
@Composable
fun MarkdownDocumentView(
    document: MarkdownDocument,
    modifier: Modifier = Modifier,
    contentPaddingValues: PaddingValues = PaddingValues(Spacings.medium),
    linkClickStrategy: LinkClickStrategy = LinkClickStrategy.Default,
    elementContent: @Composable (MarkdownBlock.Element) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacings.medium),
        contentPadding = contentPaddingValues,
    ) {
        document.metadata.title?.let { title ->
            item(key = "metadata-title") {
                Text(text = title, style = TextStyles.headlineLarge.bold())
            }
        }
        items(document.blocks, key = { it.id ?: it.hashCode() }) { block ->
            MarkdownBlockView(
                block = block,
                linkClickStrategy = linkClickStrategy,
                elementContent = elementContent,
            )
        }
    }
}

/**
 * Renders a single [MarkdownBlock].
 *
 * [MarkdownBlock.Markdown] blocks are parsed into typed [MarkdownNode]s and rendered by
 * [MarkdownTextBlock]. [MarkdownBlock.Element] blocks are forwarded to [elementContent] for
 * domain-specific rendering.
 */
@Composable
fun MarkdownBlockView(
    block: MarkdownBlock,
    modifier: Modifier = Modifier,
    linkClickStrategy: LinkClickStrategy = LinkClickStrategy.Default,
    elementContent: @Composable (MarkdownBlock.Element) -> Unit = {},
) {
    when (block) {
        is MarkdownBlock.Markdown -> MarkdownTextBlock(
            block = block,
            modifier = modifier,
            linkClickStrategy = linkClickStrategy,
        )
        is MarkdownBlock.Element -> elementContent(block)
    }
}

// --- Previews ---

@ThemePreviews
@Composable
private fun MarkdownDocumentViewPreview() {
    SpeziTheme {
        MarkdownDocumentView(document = previewDocument)
    }
}

@Preview(showBackground = true)
@Composable
private fun MarkdownBlockTextPreview() {
    SpeziTheme {
        MarkdownBlockView(
            block = MarkdownBlock.Markdown(
                id = "intro",
                rawContents = "## Overview\nThis section introduces the study.",
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MarkdownBlockElementPreview() {
    SpeziTheme {
        MarkdownBlockView(
            block = MarkdownBlock.Element(
                name = "toggle",
                attributes = listOf(MarkdownBlock.Element.Attribute("id", "consent"))
            ),
            elementContent = { element ->
                Text(text = "<${element.name}> — custom element slot", style = TextStyles.bodyMedium)
            }
        )
    }
}

private val previewDocument = MarkdownDocument.process(
    """
    ---
    title: Study Consent
    version: 1.0.0
    ---
    # Welcome
    Please read the following carefully before proceeding.

    ## Your rights
    You may withdraw from the study at any time.
    - No penalty for withdrawal
    - All data remains confidential

    ## Eligibility
    You must meet all of the following:
    1. Be at least 18 years old
    2. Reside in an eligible country
    a. United States
    b. Canada

    Contact support@study.org or visit https://study.org for details.
    """.trimIndent()
)
