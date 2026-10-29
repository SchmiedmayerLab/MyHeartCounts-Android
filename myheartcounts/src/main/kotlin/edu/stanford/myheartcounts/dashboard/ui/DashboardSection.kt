//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.semiBold

/**
 * A section of a dashboard: any content, optionally headed by a title and followed by a footnote.
 *
 * The title and the footnote are inset to line up with the content of the section's tiles and
 * cards; the footnote is drawn muted.
 *
 * @param title the heading above the content; omitted when there is none
 * @param content the content of the section, filling its width
 * @param footnote a short note below the content; omitted when there is none
 */
@Immutable
data class DashboardSection(
    val title: StringResource?,
    val content: ComposableContent,
    val footnote: StringResource?,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Spacings.small),
        ) {
            if (title != null) {
                Text(
                    text = title.text(),
                    style = TextStyles.titleMedium.semiBold(),
                    color = Colors.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = Spacings.medium)
                        .semantics { heading() },
                )
            }
            content.Content(modifier = Modifier.fillMaxWidth())
            if (footnote != null) {
                Text(
                    text = footnote.text(),
                    style = TextStyles.bodySmall,
                    color = Colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacings.medium),
                )
            }
        }
    }
}

private class DashboardSectionPreviewProvider : PreviewParameterProvider<DashboardSection> {

    private val base = DashboardSection(
        title = StringResource("Title"),
        content = GaugeCard(
            gauge = Gauge(
                progress = 0.7f,
                colors = DashboardColorPalette.redToGreen,
                lineWidth = Gauge.defaultLineWidth * 2,
                size = Gauge.Size.Large,
                label = Gauge.Label(
                    value = StringResource("70"),
                    caption = Gauge.Caption.Range(
                        minimum = StringResource("0"),
                        maximum = StringResource("100"),
                    ),
                ),
            ),
        ),
        footnote = StringResource("A footnote explaining the section, long enough to wrap onto a second line."),
    )

    override val values: Sequence<DashboardSection> = sequenceOf(
        base,
        base.copy(title = null),
        base.copy(footnote = null),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(DashboardSectionPreviewProvider::class) content: DashboardSection,
) {
    GroveTheme {
        content.Content(modifier = Modifier.padding(Spacings.medium))
    }
}
