//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.sample.app.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

data class PermissionStatus(
    val granted: Boolean,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        Text(
            modifier = Modifier
                .background(if (granted) Color.Green else Color.Red, shape = RoundedCornerShape(Spacings.extraSmall))
                .padding(Spacings.tiny),
            color = Color.White,
            text = if (granted) "GRANTED" else "NOT GRANTED",
            style = TextStyles.labelSmall
        )
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    GroveTheme {
        PermissionStatus(
            granted = false,
        ).Content()
    }
}
