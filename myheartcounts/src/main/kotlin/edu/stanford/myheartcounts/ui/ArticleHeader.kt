//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import edu.stanford.myheartcounts.MHCDrawables
import edu.stanford.myheartcounts.dashboard.ui.DashboardColorPalette
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.bold
import org.grovealliance.ui.theme.resolve

/**
 * An article's image with its title, publication date and tags overlaid along the bottom.
 */
@Immutable
data class ArticleHeader(
    val title: StringResource,
    val date: StringResource?,
    val tags: List<StringResource>,
    val image: HeaderImage?,
) : ComposableContent {

    sealed interface HeaderImage {
        data object Stanford : HeaderImage
        data class Remote(val url: String) : HeaderImage
    }

    @Composable
    override fun Content(modifier: Modifier) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = MIN_HEADER_HEIGHT)
                .clipToBounds(),
            contentAlignment = Alignment.BottomStart,
        ) {
            when (val image = image) {
                HeaderImage.Stanford -> Image(
                    painter = painterResource(id = MHCDrawables.img_stanford),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
                is HeaderImage.Remote -> AsyncImage(
                    model = image.url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
                null -> Unit
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Colors.surfaceContainer.copy(alpha = OVERLAY_ALPHA))
                    .padding(Spacings.medium),
                verticalArrangement = Arrangement.spacedBy(Spacings.small),
            ) {
                Text(text = title.text(), style = TextStyles.headlineMedium.bold())
                if (date != null || tags.isNotEmpty()) Metadata()
            }
        }
    }

    @Composable
    private fun Metadata() {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacings.small),
        ) {
            date?.let {
                Text(
                    text = it.text(),
                    style = TextStyles.bodyMedium,
                    color = Colors.onSurfaceVariant,
                )
            }
            if (tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacings.small),
                    verticalArrangement = Arrangement.spacedBy(Spacings.extraSmall),
                ) {
                    tags.forEach { tag ->
                        Text(
                            text = tag.text(),
                            style = TextStyles.labelMedium,
                            color = Color.White,
                            modifier = Modifier
                                .background(DashboardColorPalette.blue.resolve(), GroveShapes.medium)
                                .padding(horizontal = Spacings.small, vertical = Spacings.extraSmall),
                        )
                    }
                }
            }
        }
    }

    private companion object {
        val MIN_HEADER_HEIGHT = 271.dp
        const val OVERLAY_ALPHA = 0.9f
    }
}

private class ArticleHeaderPreviewProvider : PreviewParameterProvider<ArticleHeader> {
    private val base = ArticleHeader(
        title = StringResource(text = "Welcome to My Heart Counts"),
        date = StringResource(text = "Jan 1, 2026"),
        tags = listOf(StringResource(text = "Study"), StringResource(text = "Heart Health")),
        image = ArticleHeader.HeaderImage.Stanford,
    )

    override val values = sequenceOf(
        base,
        base.copy(date = null, tags = emptyList(), image = null),
        base.copy(tags = emptyList(), image = null),
        base.copy(date = null, image = null),
        base.copy(
            title = StringResource(text = "What comes next in your cardiovascular health study journey?"),
            tags = listOf(
                StringResource(text = "Study Updates"),
                StringResource(text = "Physical Activity"),
                StringResource(text = "Heart Health"),
            ),
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(ArticleHeaderPreviewProvider::class) content: ArticleHeader) {
    GroveTheme {
        Surface { content.Content() }
    }
}
