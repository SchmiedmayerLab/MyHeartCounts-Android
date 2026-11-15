//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.debug

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import edu.stanford.myheartcounts.featureflag.FeatureFlag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.grovealliance.health.HealthConnectAvailability
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.groveSectionList
import org.junit.Test

class DebugSheetScreenshotTest : ScreenshotTest() {

    @Test
    fun `DebugSheet screenshot`() = runTest {
        // given
        val sheet = sheet(scope = backgroundScope)
        val loaded = sheet.copy(sections = MutableStateFlow(sheet.sections.first { it.sections.size == LOADED_SECTION_COUNT }))

        // when / then
        screenshot { loaded.Content(modifier = Modifier.fillMaxSize()) }
    }

    @Test
    fun `DebugSheet health section screenshot`() = runTest {
        // given
        val health = sheet(scope = backgroundScope).loadedSection(key = "health")
        val list = groveSectionList { section(health) }

        // when / then
        screenshot { list.Content(modifier = Modifier.fillMaxSize()) }
    }

    private fun sheet(scope: CoroutineScope) = DebugMapperImpl().map(
        input = DebugMapper.Input(
            appInfo = flowOf(
                DebugAppInfo(
                    version = "1.2.0",
                    build = "42",
                    firebaseProjectId = "myheart-counts-development",
                    accountId = "rnzv8nCjZZLFvJIHcPRzcBnjMoH4",
                    enrolledStudyRevision = "44",
                    bundledStudyRevision = "44",
                    lastSignedConsentVersion = null,
                ),
            ),
            healthInfo = flowOf(
                DebugHealthInfo(
                    availability = HealthConnectAvailability.Available,
                    isFullyAuthorized = false,
                    historyAccess = true,
                    backgroundAccess = false,
                    read = listOf(
                        DebugHealthInfo.TypeAccess(name = "Steps", isGranted = true),
                        DebugHealthInfo.TypeAccess(name = "Weight", isGranted = true),
                        DebugHealthInfo.TypeAccess(name = "HeartRate", isGranted = false),
                    ),
                    write = listOf(
                        DebugHealthInfo.TypeAccess(name = "Weight", isGranted = true),
                        DebugHealthInfo.TypeAccess(name = "Height", isGranted = false),
                    ),
                ),
            ),
            scope = scope,
            shakeToOpen = MutableStateFlow(true),
            flagValues = mapOf(FeatureFlag.SimplifyOnboarding to MutableStateFlow(true)),
            actionSink = ActionSource<DebugMapper.Action> {}.sink(),
        ),
    )

    private companion object {
        const val LOADED_SECTION_COUNT = 4
    }
}
