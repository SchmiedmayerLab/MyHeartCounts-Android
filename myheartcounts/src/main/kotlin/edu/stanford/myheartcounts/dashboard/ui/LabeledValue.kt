//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.toUpperCase
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
 * A value under a small label naming it, the label drawn muted and in capitals.
 *
 * @param label what the value describes
 * @param value the value itself, already formatted
 */
@Immutable
data class LabeledValue(
    val label: StringResource,
    val value: StringResource,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier.semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(Spacings.extraSmall),
        ) {
            Text(
                text = label.text().toUpperCase(LocaleList.current),
                style = TextStyles.labelMedium,
                color = Colors.onSurfaceVariant,
            )
            Text(
                text = value.text(),
                style = TextStyles.titleMedium.semiBold(),
                color = Colors.onSurface,
            )
        }
    }
}

private class LabeledValuePreviewProvider : PreviewParameterProvider<LabeledValue> {

    private val base = LabeledValue(
        label = StringResource("Label"),
        value = StringResource("20 October 2026"),
    )

    override val values: Sequence<LabeledValue> = sequenceOf(
        base,
        base.copy(value = StringResource("A value long enough to wrap onto a second line")),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(LabeledValuePreviewProvider::class) content: LabeledValue,
) {
    GroveTheme {
        content.Content(modifier = Modifier.padding(Spacings.medium))
    }
}
