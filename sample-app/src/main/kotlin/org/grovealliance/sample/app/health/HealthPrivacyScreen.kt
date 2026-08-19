//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.sample.app.health

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.grovealliance.sample.app.R
import org.grovealliance.ui.StaticGroveScaffold
import org.grovealliance.ui.rememberGroveAppBar
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles

@Composable
fun HealthPrivacyScreen() {
    val activity = LocalActivity.current
    val appBar = rememberGroveAppBar(activity) {
        title(R.string.app_name)
        back { activity?.finish() }
    }
    StaticGroveScaffold(appBar = appBar) {
        Text(
            modifier = Modifier.padding(Spacings.medium),
            text = "This app uses Health Connect to read and write health data for demo purposes.",
            style = TextStyles.bodyMedium
        )
    }
}
