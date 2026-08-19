//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

/**
 * Composable for displaying a labeled horizontal divider.
 *
 * @param label The label to display.
 */
data class LabeledHorizontalDivider(
    val label: StringResource,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacings.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = label.text(),
                textAlign = TextAlign.Center,
                style = TextStyles.bodyMedium,
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    GroveTheme {
        LabeledHorizontalDivider(label = StringResource("or")).Content()
    }
}
