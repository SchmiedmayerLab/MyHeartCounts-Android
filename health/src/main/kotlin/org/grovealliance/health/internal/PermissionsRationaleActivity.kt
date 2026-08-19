//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.internal

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.grovealliance.core.dependency
import org.grovealliance.health.Health
import org.grovealliance.resources.Strings
import org.grovealliance.ui.GroveScaffold
import org.grovealliance.ui.rememberGroveAppBar
import org.grovealliance.ui.rememberMutableGroveScaffoldState
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles

internal class PermissionsRationaleActivity : AppCompatActivity() {
    private val health by dependency<Health>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GroveTheme {
                Screen(config = health.privacyConfig)
            }
        }
    }

    @Composable
    private fun Screen(config: PrivacyConfig) {
        when (config) {
            is PrivacyConfig.Text -> RationaleScreen(
                title = config.title.text(),
                description = config.description.text()
            )
            is PrivacyConfig.Composable -> config.composable.invoke()
            is PrivacyConfig.Content -> config.content.Content()
            is PrivacyConfig.Default -> RationaleScreen(
                title = stringResource(Strings.default_privacy_policy_title),
                description = stringResource(Strings.default_privacy_policy_description),
            )
        }
    }

    @Composable
    private fun RationaleScreen(
        title: String,
        description: String,
    ) {
        val appBar = rememberGroveAppBar(title) {
            title(title)
            back { finish() }
        }
        val scaffoldState = rememberMutableGroveScaffoldState(appBar = appBar)
        GroveScaffold(state = scaffoldState) {
            Column(
                modifier = Modifier
                    .padding(Spacings.medium)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = description,
                    style = TextStyles.bodyMedium
                )
            }
        }
    }
}
