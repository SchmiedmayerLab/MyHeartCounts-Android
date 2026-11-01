//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.debug

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import edu.stanford.myheartcounts.ui.ToggleSection
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.junit.Test

class DebugSheetScreenshotTest : ScreenshotTest() {

    private val shake = DebugShakeSection(
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
    )

    private val featureFlags = DebugFeatureFlagsSection(
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
    )

    @Test
    fun `DebugSheet screenshot`() {
        val sheet = DebugSheet(
            appBar = groveAppBar {
                title(StringResource("Debug"))
                close {}
            },
            shake = shake,
            featureFlags = featureFlags,
            appInfo = MutableStateFlow(
                DebugAppInfoSection(
                    title = DebugSectionTitle(title = StringResource("App Info")),
                    cards = listOf(
                        card("Version" to "1.2.0", "Build" to "42"),
                        card("Project ID" to "myheart-counts-development", "Account ID" to "rnzv8nCjZZLFvJIHcPRzcBnjMoH4"),
                        card("Study Revision (enrolled)" to "44", "Study Revision (bundled)" to "44", "lastSignedConsentVersion" to null),
                    ),
                ),
            ),
            health = MutableStateFlow(
                DebugHealthSection(
                    title = DebugSectionTitle(title = StringResource("Health")),
                    status = card(
                        "Health Connect" to "Available",
                        "All permissions granted" to "No",
                        "History access" to "Yes",
                        "Background access" to "No",
                    ),
                    granted = DebugTextCard(
                        title = StringResource("Granted"),
                        text = StringResource("Steps (R), Weight (RW)"),
                    ),
                    notGranted = DebugTextCard(
                        title = StringResource("Not granted"),
                        text = StringResource("HeartRate (R), Height (W)"),
                    ),
                ),
            ),
            onDismiss = {},
        )

        screenshot {
            sheet.Content(modifier = Modifier.fillMaxSize())
        }
    }

    private fun card(vararg rows: Pair<String, String?>) = DebugRowsCard(
        rows = rows.map { (label, value) ->
            DebugRowsCard.Row(
                label = StringResource(label),
                value = value,
            )
        },
    )
}
