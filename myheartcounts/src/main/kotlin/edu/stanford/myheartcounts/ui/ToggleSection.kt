//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A card with a [title], an optional [subtitle] and a switch showing [isOn]. Tapping anywhere on the
 * card reports the requested value through [onToggle]; the switch follows [isOn] only.
 */
data class ToggleSection(
    val title: StringResource,
    val subtitle: StringResource?,
    val isOn: StateFlow<Boolean>,
    val onToggle: (Boolean) -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val isOn by isOn.collectAsStateWithLifecycle()

        GroveCard(modifier = modifier) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = isOn,
                        role = Role.Switch,
                        onValueChange = onToggle,
                    )
                    .padding(Spacings.medium),
                horizontalArrangement = Arrangement.spacedBy(Spacings.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacings.tiny),
                ) {
                    Text(
                        text = title.text(),
                        style = TextStyles.bodyLarge,
                        color = Colors.onSurface,
                    )
                    subtitle?.let {
                        Text(
                            text = it.text(),
                            style = TextStyles.bodySmall,
                            color = Colors.onSurfaceVariant,
                        )
                    }
                }
                Switch(
                    checked = isOn,
                    onCheckedChange = null,
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    GroveTheme {
        ToggleSection(
            title = StringResource("Weekly Summary"),
            subtitle = StringResource("Receive a summary of your activity every Monday."),
            isOn = MutableStateFlow(value = true),
            onToggle = {},
        ).Content(modifier = Modifier.padding(Spacings.medium))
    }
}
