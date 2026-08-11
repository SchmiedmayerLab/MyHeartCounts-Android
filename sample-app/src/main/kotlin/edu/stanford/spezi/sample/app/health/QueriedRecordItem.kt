//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.sample.app.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.SpeziCard
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Spacings
import edu.stanford.spezi.ui.theme.SpeziTheme
import edu.stanford.spezi.ui.theme.TextStyles
import edu.stanford.spezi.ui.theme.ThemePreviews
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

data class QueriedRecordItem(
    val title: StringResource,
    val description: Flow<StringResource?>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        SpeziCard(modifier = modifier.fillMaxWidth()) {
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
    SpeziTheme {
        val item = QueriedRecordItem(
            title = StringResource("Sample Queried Record"),
            description = flowOf(StringResource("123 records found."))
        )
        item.Content()
    }
}
