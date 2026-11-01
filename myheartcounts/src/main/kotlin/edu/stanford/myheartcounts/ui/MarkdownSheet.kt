//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.grovealliance.markdown.MarkdownBlockView
import org.grovealliance.markdown.MarkdownDocument
import org.grovealliance.ui.BottomSheetComposableContent
import org.grovealliance.ui.DisplayedEffect
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.OnActionVoid
import org.grovealliance.ui.StaticGroveScaffold
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A bottom sheet rendering an article's [header] and [document] beneath the given [appBar].
 *
 * [onDisplayed] is invoked each time the sheet becomes visible, so that presenting the document can
 * be recorded.
 */
data class MarkdownSheet(
    val appBar: GroveAppBar,
    val document: MarkdownDocument,
    val header: ArticleHeader,
    val onDisplayed: OnActionVoid = {},
) : BottomSheetComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        DisplayedEffect(onDisplayed = onDisplayed)
        StaticGroveScaffold(appBar = appBar) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacings.medium),
                contentPadding = PaddingValues(bottom = Spacings.medium),
            ) {
                item(key = "article-header") {
                    Column {
                        header.Content()
                        HorizontalDivider()
                    }
                }
                items(document.blocks) { block ->
                    MarkdownBlockView(
                        block = block,
                        modifier = Modifier.padding(horizontal = Spacings.medium),
                    )
                }
            }
        }
    }
}

private class MarkdownSheetPreviewProvider : PreviewParameterProvider<MarkdownSheet> {
    override val values = sequenceOf(
        MarkdownSheet(
            appBar = groveAppBar { close { } },
            header = ArticleHeader(
                title = StringResource(text = "Welcome to My Heart Counts"),
                date = StringResource(text = "Jan 1, 2026"),
                tags = listOf(StringResource(text = "Study"), StringResource(text = "Heart Health")),
                image = ArticleHeader.HeaderImage.Stanford,
            ),
            document = MarkdownDocument.process(
                text = """
                Your contributions help advance our understanding of cardiovascular health.

                ## Surveys
                - Complete your daily surveys when you can.
                - A survey you miss stays available for later completion.
                """.trimIndent(),
            ),
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(MarkdownSheetPreviewProvider::class) content: MarkdownSheet) {
    GroveTheme { content.Sheet(modifier = Modifier.fillMaxSize()) }
}
