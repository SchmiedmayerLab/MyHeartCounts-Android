//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import edu.stanford.myheartcounts.MHCStrings
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Spacings
import org.junit.Test

class DataProcessingRowScreenshotTest : ScreenshotTest() {

    @Test
    fun `DataProcessingRow screenshot`() {
        // given
        val row = DataProcessingRow(text = StringResource(MHCStrings.account_processing_health_data))

        // when / then
        screenshot {
            GroveCard(modifier = Modifier.padding(Spacings.medium)) {
                row.Content()
            }
        }
    }
}
