//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A row showing a paragraph of [text].
 */
data class TextRow(
    val text: StringResource,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Text(
            modifier = modifier.padding(Spacings.medium),
            text = text.text(),
            color = Colors.onSurfaceVariant,
        )
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    GroveTheme {
        TextRow(
            text = StringResource("Answer a few questions to find out whether you can take part in the study."),
        ).Content(modifier = Modifier.fillMaxWidth())
    }
}
