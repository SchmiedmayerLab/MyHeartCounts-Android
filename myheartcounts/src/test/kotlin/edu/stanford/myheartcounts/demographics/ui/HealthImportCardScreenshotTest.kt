//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT
package edu.stanford.myheartcounts.demographics.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import edu.stanford.myheartcounts.MHCStrings
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Spacings
import org.junit.Test

class HealthImportCardScreenshotTest : ScreenshotTest() {
    private val base = HealthImportCard(
        title = StringResource(MHCStrings.demographics_health_import_title),
        description = StringResource(MHCStrings.demographics_health_import_description),
        onClick = {},
    )

    @Test
    fun `HealthImportCard screenshot`() {
        // when / then
        screenshot {
            base.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }
}
