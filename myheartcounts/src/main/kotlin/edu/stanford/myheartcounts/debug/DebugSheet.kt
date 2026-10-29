//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.myheartcounts.ui.ToggleSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.BottomSheetComposableContent
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.StaticGroveScaffold
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A bottom sheet for development and support, beneath [appBar]: the [shake] section deciding
 * whether a shake opens it, the [featureFlags] section, then
 * the [appInfo] and [health] sections once they are known, both followed while the sheet is shown.
 * [onDismiss] is called once the sheet is swiped or tapped away.
 */
data class DebugSheet(
    val appBar: GroveAppBar,
    val shake: DebugShakeSection,
    val featureFlags: DebugFeatureFlagsSection,
    val appInfo: StateFlow<DebugAppInfoSection?>,
    val health: StateFlow<DebugHealthSection?>,
    override val onDismiss: () -> Unit,
) : BottomSheetComposableContent {

    override val skipPartiallyExpanded: Boolean = true

    @Composable
    override fun Content(modifier: Modifier) {
        val appInfoSection by appInfo.collectAsStateWithLifecycle()
        val healthSection by health.collectAsStateWithLifecycle()

        StaticGroveScaffold(appBar = appBar) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(Spacings.medium),
                verticalArrangement = Arrangement.spacedBy(Spacings.medium),
            ) {
                shake.Content(modifier = Modifier.fillMaxWidth())
                featureFlags.Content(modifier = Modifier.fillMaxWidth())

                appInfoSection?.Content(modifier = Modifier.fillMaxWidth())

                healthSection?.Content(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    GroveTheme {
        DebugSheet(
            appBar = groveAppBar { title(StringResource("Debug")) },
            shake = DebugShakeSection(
                title = DebugSectionTitle(title = StringResource("Debug Sheet")),
                toggle = ToggleSection(
                    title = StringResource("Open on Shake"),
                    subtitle = StringResource(
                        "Does this sheet keep popping up while you're running or moving? " +
                            "Turn this off to stop shaking from opening it. To turn it back on, relaunch the app.",
                    ),
                    isOn = MutableStateFlow(value = true),
                    onToggle = {},
                ),
            ),
            featureFlags = DebugFeatureFlagsSection(
                title = DebugSectionTitle(title = StringResource("Feature Flags")),
                flags = listOf(
                    ToggleSection(
                        title = StringResource("Simplify Onboarding"),
                        subtitle = StringResource("Shortens onboarding to login, Health Connect access and enrollment."),
                        isOn = MutableStateFlow(value = true),
                        onToggle = {},
                    ),
                ),
                note = StringResource("Changes apply once the app restarts."),
                restartButton = AsyncTextButton(
                    title = StringResource("Restart"),
                    action = {},
                ),
            ),
            appInfo = MutableStateFlow(
                DebugAppInfoSection(
                    title = DebugSectionTitle(title = StringResource("App Info")),
                    cards = listOf(
                        DebugRowsCard(
                            rows = listOf(
                                DebugRowsCard.Row(
                                    label = StringResource("Version"),
                                    value = "1.2.0",
                                ),
                                DebugRowsCard.Row(
                                    label = StringResource("Account ID"),
                                    value = null,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            health = MutableStateFlow(null),
            onDismiss = {},
        ).Content(modifier = Modifier)
    }
}
