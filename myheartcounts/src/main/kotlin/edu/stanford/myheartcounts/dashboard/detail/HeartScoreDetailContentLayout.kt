//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import edu.stanford.myheartcounts.dashboard.ui.DashboardSection
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.bold

/**
 * A factor's details, one section below the other.
 */
@Immutable
data class HeartScoreDetailContentLayout(
    val title: StringResource,
    val sections: List<DashboardSection>,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacings.medium),
            verticalArrangement = Arrangement.spacedBy(Spacings.large),
        ) {
            item { Text(text = title.text(), style = TextStyles.headlineLarge.bold()) }
            items(items = sections) { section ->
                section.Content(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
