//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Spacings
import org.junit.Test

class LabeledValueScreenshotTest : ScreenshotTest() {

    private val base = LabeledValue(
        label = StringResource("Label"),
        value = StringResource("20 October 2026"),
    )

    @Test
    fun `LabeledValue screenshot`() {
        screenshot(value = base)
    }

    @Test
    fun `LabeledValue long value screenshot`() {
        screenshot(value = base.copy(value = StringResource("A value long enough to wrap onto a second line")))
    }

    private fun screenshot(value: LabeledValue) {
        screenshot {
            value.Content(
                modifier = Modifier
                    .padding(Spacings.medium)
                    .width(VALUE_WIDTH),
            )
        }
    }

    private companion object {
        val VALUE_WIDTH = 180.dp
    }
}
