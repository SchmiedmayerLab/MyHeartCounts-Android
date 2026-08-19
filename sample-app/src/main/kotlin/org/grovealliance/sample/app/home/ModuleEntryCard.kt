//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.sample.app.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

data class ModuleEntryCard(
    val title: StringResource,
    val description: StringResource,
    val onClick: () -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        GroveCard(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Column(
                modifier = Modifier.padding(Spacings.medium),
                verticalArrangement = Arrangement.spacedBy(Spacings.extraSmall)
            ) {
                Text(
                    text = title.text(),
                    style = TextStyles.titleMedium
                )
                Text(
                    text = description.text(),
                    style = TextStyles.bodyMedium,
                )
            }
        }
    }
}

@Composable
@ThemePreviews
private fun Preview() {
    GroveTheme {
        ModuleEntryCard(
            title = StringResource("Sample Module"),
            description = StringResource("This is a sample module description."),
            onClick = {}
        ).Content()
    }
}
