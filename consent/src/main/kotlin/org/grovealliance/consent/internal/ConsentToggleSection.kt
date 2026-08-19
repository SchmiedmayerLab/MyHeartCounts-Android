//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.consent.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A consent form section presenting a labelled boolean toggle.
 *
 * When [expectedValue] is non-null the toggle must match it for the form to be considered complete.
 */
internal data class ConsentToggleSection(
    val id: String,
    val text: String,
    val initialValue: Boolean,
    val expectedValue: Boolean?,
    val checked: Flow<Boolean>,
    val onValueChanged: (Boolean) -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        GroveCard(modifier = modifier) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacings.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = text,
                    style = TextStyles.bodyMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = Spacings.medium),
                )
                Switch(
                    checked = checked.collectAsStateWithLifecycle(false).value,
                    onCheckedChange = onValueChanged,
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ConsentToggleContentPreview() {
    GroveTheme {
        ConsentToggleSection(
            id = "future-studies",
            text = "May we contact you about future studies that may be of interest to you?",
            initialValue = true,
            expectedValue = null,
            checked = MutableStateFlow(true),
            onValueChanged = {},
        ).Content(modifier = Modifier.fillMaxWidth())
    }
}
