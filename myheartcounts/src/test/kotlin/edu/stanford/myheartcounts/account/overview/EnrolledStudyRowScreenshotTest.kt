//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.ui.ExternalLink
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Spacings
import org.junit.Test

class EnrolledStudyRowScreenshotTest : ScreenshotTest() {

    private val base = EnrolledStudyRow(
        title = "My Heart Counts",
        shortExplanation = "Improve your cardiovascular health",
        enrolledSince = StringResource(MHCStrings.account_enrolled_since, "Jan 1, 2026, 9:30 AM"),
        website = ExternalLink(
            text = StringResource(MHCStrings.account_study_website),
            onClicked = {},
        ),
    )

    @Test
    fun `EnrolledStudyRow enrolled screenshot`() {
        // when / then
        screenshot(row = base)
    }

    @Test
    fun `EnrolledStudyRow without explanation screenshot`() {
        // given
        val row = base.copy(shortExplanation = null)

        // when / then
        screenshot(row = row)
    }

    private fun screenshot(row: EnrolledStudyRow) {
        screenshot {
            GroveCard(modifier = Modifier.padding(Spacings.medium)) {
                row.Content(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
