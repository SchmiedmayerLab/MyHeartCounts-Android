//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.sample.app.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.theme.Spacings
import edu.stanford.spezi.ui.theme.TextStyles

data class QueriesSection(
    val title: String,
    val queries: List<QueriedRecordItem>,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Spacings.small)
        ) {
            Text(
                text = title,
                style = TextStyles.headlineSmall
            )

            queries.forEach { item ->
                item.Content()
            }
        }
    }
}
