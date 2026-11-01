//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * The debug sheet's section about Health Connect, under its [title]: its [status], and the data
 * types with [granted] and with [notGranted] access.
 */
data class DebugHealthSection(
    val title: DebugSectionTitle,
    val status: DebugRowsCard,
    val granted: DebugTextCard,
    val notGranted: DebugTextCard,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Spacings.medium),
        ) {
            title.Content()
            status.Content(modifier = Modifier.fillMaxWidth())
            granted.Content(modifier = Modifier.fillMaxWidth())
            notGranted.Content(modifier = Modifier.fillMaxWidth())
        }
    }
}

private class DebugHealthSectionPreviewProvider : PreviewParameterProvider<DebugHealthSection> {
    private val base = DebugHealthSection(
        title = DebugSectionTitle(title = StringResource("Health")),
        status = DebugRowsCard(
            rows = listOf(
                DebugRowsCard.Row(
                    label = StringResource("Health Connect"),
                    value = "Available",
                ),
                DebugRowsCard.Row(
                    label = StringResource("All permissions granted"),
                    value = "No",
                ),
            ),
        ),
        granted = DebugTextCard(
            title = StringResource("Granted"),
            text = StringResource("Steps (R), Weight (RW)"),
        ),
        notGranted = DebugTextCard(
            title = StringResource("Not granted"),
            text = StringResource("HeartRate (R), Height (W)"),
        ),
    )

    override val values = sequenceOf(
        base,
        base.copy(notGranted = base.notGranted.copy(text = StringResource("None"))),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(DebugHealthSectionPreviewProvider::class) content: DebugHealthSection) {
    GroveTheme { content.Content(modifier = Modifier.fillMaxWidth()) }
}
