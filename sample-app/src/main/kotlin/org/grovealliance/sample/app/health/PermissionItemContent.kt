//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.sample.app.health

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

data class PermissionItemContent(
    val title: String,
    val status: PermissionStatus,
    val action: PermissionAction?,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        GroveCard {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(Spacings.medium),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacings.small)) {
                    Text(
                        text = title,
                        style = TextStyles.titleMedium
                    )
                    status.Content()
                }

                action?.let {
                    val activity = LocalActivity.current as? FragmentActivity ?: return@let
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { action.onClick(activity) }) {
                        Text(text = "Request")
                    }
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    GroveTheme {
        val content = PermissionItemContent(
            title = "Health Data Permission",
            status = PermissionStatus(granted = true),
            action = PermissionAction(onClick = {})
        )

        content.Content()
    }
}
