//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.sample.app.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

data class QueriedRecordItem(
    val title: StringResource,
    val description: Flow<StringResource?>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        GroveCard(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(Spacings.medium),
                verticalArrangement = Arrangement.spacedBy(Spacings.small)
            ) {
                Text(
                    text = title.text(),
                    style = TextStyles.titleMedium
                )
                val description = description.collectAsState(initial = null).value
                description?.let {
                    Text(
                        text = it.text(),
                        style = TextStyles.titleMedium
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    GroveTheme {
        val item = QueriedRecordItem(
            title = StringResource("Sample Queried Record"),
            description = flowOf(StringResource("123 records found."))
        )
        item.Content()
    }
}
