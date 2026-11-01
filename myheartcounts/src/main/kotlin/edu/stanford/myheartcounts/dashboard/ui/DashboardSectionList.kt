//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
 * A scrolling list of [DashboardSection]s, in the order given.
 */
data class DashboardSectionList(
    val sections: List<DashboardSection>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacings.medium),
            verticalArrangement = Arrangement.spacedBy(Spacings.large),
        ) {
            items(items = sections) { section ->
                section.Content(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private class DashboardSectionListPreviewProvider : PreviewParameterProvider<DashboardSectionList> {
    private val base = DashboardSectionList(
        sections = listOf(
            DashboardSection(
                title = StringResource("Enrollment"),
                content = LabeledValue(
                    label = StringResource("Enrolled since"),
                    value = StringResource("January 1, 2026"),
                ),
                footnote = StringResource("Counted from the day you enrolled."),
            ),
        ),
    )

    override val values = sequenceOf(base)
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(DashboardSectionListPreviewProvider::class) content: DashboardSectionList) {
    GroveTheme {
        content.Content()
    }
}
