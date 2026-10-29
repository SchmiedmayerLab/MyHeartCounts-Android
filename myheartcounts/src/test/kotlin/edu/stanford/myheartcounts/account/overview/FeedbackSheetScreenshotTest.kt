//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.theme.Colors
import org.junit.Test

class FeedbackSheetScreenshotTest : ScreenshotTest() {

    @Test
    fun `FeedbackSheet empty screenshot`() {
        val sheet = sheet(message = "")

        screenshot {
            sheet.Content(modifier = Modifier.fillMaxSize())
        }
    }

    @Test
    fun `FeedbackSheet written screenshot`() {
        val sheet = sheet(message = "The new Heart Health dashboard is great, but I'd love to see my weekly step trend too.")

        screenshot {
            sheet.Content(modifier = Modifier.fillMaxSize())
        }
    }

    private fun sheet(message: String): FeedbackSheet {
        val canSend = message.isNotBlank()
        return FeedbackSheet(
            appBar = MutableStateFlow(
                groveAppBar {
                    title(StringResource("Feedback"))
                    close {}
                    action(
                        content = AsyncTextButton(
                            title = StringResource("Send"),
                            enabled = canSend,
                            containerColor = { Colors.transparent },
                            textColor = { if (canSend) Colors.primary else Colors.onSurfaceVariant },
                            action = {},
                        ),
                    )
                },
            ),
            message = MutableStateFlow(message),
            onMessageChanged = {},
            isDismissible = { true },
        )
    }
}
