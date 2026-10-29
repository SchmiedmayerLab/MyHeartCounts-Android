//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.bold

/**
 * A labeled value beside a [Gauge], set apart by a divider, drawn without a surface of its own.
 *
 * The value is made of one or more parts, each optionally followed by a smaller, muted unit, such
 * as a duration of hours and minutes. Without a value, a muted text stands in for it.
 *
 * @param label what the value describes
 * @param value the value, or the text standing in for a missing one
 * @param footnote a qualifier below the value, drawn muted; omitted when there is none
 * @param gauge the gauge at the end, drawn at its own size
 */
@Immutable
data class GaugeHeader(
    val label: StringResource,
    val value: Value,
    val footnote: StringResource?,
    val gauge: Gauge,
) : ComposableContent {

    /**
     * The value of a [GaugeHeader].
     */
    sealed interface Value {
        /**
         * A value made of [parts], drawn side by side.
         *
         * @param parts the parts, from start to end; must not be empty
         */
        data class Measured(val parts: List<Part>) : Value {
            init {
                require(parts.isNotEmpty()) { "parts must not be empty" }
            }
        }

        /**
         * A missing value, with a muted text standing in for it.
         *
         * @param text the text standing in for the value
         */
        data class Missing(val text: StringResource) : Value
    }

    /**
     * A part of a [Value.Measured].
     *
     * @param text the part itself, already formatted
     * @param unit the unit following the part, drawn smaller and muted; omitted when there is none
     */
    data class Part(
        val text: StringResource,
        val unit: StringResource?,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val valueStyle = TextStyles.titleMedium.bold()
        Row(
            modifier = modifier
                .padding(horizontal = Spacings.medium)
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Spacings.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(Spacings.extraSmall),
            ) {
                Text(
                    text = label.text(),
                    style = TextStyles.bodyMedium,
                    color = Colors.onSurfaceVariant,
                )
                when (value) {
                    is Value.Measured -> Row(horizontalArrangement = Arrangement.spacedBy(Spacings.small)) {
                        value.parts.forEach { part ->
                            Row(
                                modifier = Modifier.alignByBaseline(),
                                horizontalArrangement = Arrangement.spacedBy(Spacings.tiny),
                            ) {
                                Text(
                                    text = part.text.text(),
                                    style = valueStyle,
                                    color = Colors.onSurface,
                                    modifier = Modifier.alignByBaseline(),
                                )
                                if (part.unit != null) {
                                    Text(
                                        text = part.unit.text(),
                                        style = TextStyles.bodyMedium,
                                        color = Colors.onSurfaceVariant,
                                        modifier = Modifier.alignByBaseline(),
                                    )
                                }
                            }
                        }
                    }
                    is Value.Missing -> Text(
                        text = value.text.text(),
                        style = valueStyle,
                        color = Colors.onSurfaceVariant,
                    )
                }
                if (footnote != null) {
                    Text(
                        text = footnote.text(),
                        style = TextStyles.bodyMedium,
                        color = Colors.onSurfaceVariant,
                    )
                }
            }
            VerticalDivider(modifier = Modifier.fillMaxHeight())
            gauge.Content()
        }
    }
}

private class GaugeHeaderPreviewProvider : PreviewParameterProvider<GaugeHeader> {

    private val base = GaugeHeader(
        label = StringResource("Label"),
        value = GaugeHeader.Value.Measured(
            parts = listOf(
                GaugeHeader.Part(
                    text = StringResource("8"),
                    unit = StringResource("hr"),
                ),
                GaugeHeader.Part(
                    text = StringResource("45"),
                    unit = StringResource("min"),
                ),
            ),
        ),
        footnote = StringResource("7:36"),
        gauge = Gauge(
            progress = 1f,
            colors = DashboardColorPalette.redToGreen,
            lineWidth = Gauge.defaultLineWidth * 1.5f,
            size = Gauge.Size.Regular,
            label = Gauge.Label(
                value = StringResource("100"),
                caption = null,
            ),
        ),
    )

    override val values: Sequence<GaugeHeader> = sequenceOf(
        base,
        base.copy(
            value = GaugeHeader.Value.Missing(text = StringResource("No Data")),
            footnote = null,
            gauge = base.gauge.copy(
                progress = null,
                colors = listOf(ColorToken.Outline),
                label = base.gauge.label?.copy(value = StringResource("—")),
            ),
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(GaugeHeaderPreviewProvider::class) content: GaugeHeader,
) {
    GroveTheme {
        content.Content(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacings.medium),
        )
    }
}
