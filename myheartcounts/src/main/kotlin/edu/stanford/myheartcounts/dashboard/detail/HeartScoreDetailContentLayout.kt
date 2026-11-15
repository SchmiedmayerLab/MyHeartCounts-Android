//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.detail

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveSection
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.VerticalSpacer
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.bold

/**
 * A factor's details, one section below the other.
 */
@Immutable
data class HeartScoreDetailContentLayout(
    val title: StringResource,
    val sections: List<GroveSection>,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        val sectionItems = sections.map { it.items() }
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacings.medium),
        ) {
            item(key = TITLE_KEY) { Text(text = title.text(), style = TextStyles.headlineLarge.bold()) }
            sectionItems.forEachIndexed { index, items ->
                item(key = "${sections[index].key}-gap") { VerticalSpacer(height = Spacings.large) }
                items()
            }
        }
    }

    private companion object {
        const val TITLE_KEY = "title"
    }
}
