//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.consentforms.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * The [pages] of a PDF one below the other, each drawn when it scrolls into view.
 */
data class ConsentFormDocumentLayout(
    val pages: List<Page>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        BoxWithConstraints(modifier = modifier) {
            val width = with(LocalDensity.current) { (maxWidth - Spacings.medium * 2).roundToPx() }.coerceAtLeast(1)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Spacings.medium),
                verticalArrangement = Arrangement.spacedBy(Spacings.medium),
            ) {
                items(pages) { page ->
                    val image by produceState<ImageBitmap?>(initialValue = null, page, width) {
                        value = page.image(width).getOrNull()
                    }
                    val pageModifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(page.aspectRatio)
                        .border(
                            width = Sizes.Border.small,
                            color = Colors.outlineVariant,
                        )
                        .background(color = Color.White)
                    image?.let {
                        Image(
                            modifier = pageModifier,
                            bitmap = it,
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                        )
                    } ?: Box(modifier = pageModifier)
                }
            }
        }
    }

    /**
     * A page of the PDF, [aspectRatio] wide for its height, whose [image] is drawn for a given
     * width in pixels.
     */
    data class Page(
        val aspectRatio: Float,
        val image: suspend (width: Int) -> Result<ImageBitmap>,
    )
}

private class ConsentFormDocumentLayoutPreviewProvider : PreviewParameterProvider<ConsentFormDocumentLayout> {
    private val page = ConsentFormDocumentLayout.Page(
        aspectRatio = 8.5f / 11f,
        image = { Result.failure(IllegalStateException("Previews draw no pages.")) },
    )
    private val base = ConsentFormDocumentLayout(pages = listOf(page, page))
    override val values = sequenceOf(base)
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(ConsentFormDocumentLayoutPreviewProvider::class) content: ConsentFormDocumentLayout,
) {
    GroveTheme { content.Content() }
}
