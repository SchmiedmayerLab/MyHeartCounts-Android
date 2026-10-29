//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.chart

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.GroveTheme
import org.junit.Rule
import org.junit.Test

class MenuPickerInteractionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun menuShowsSelectionAndReportsTheChosenIndex() {
        val selected = mutableStateOf(1)
        val base = MenuPicker(
            label = StringResource("Time range"),
            options = listOf(StringResource("Last 7 days"), StringResource("Last 14 days")),
            selectedIndex = 1,
            onSelected = { selected.value = it },
        )
        compose.setContent {
            GroveTheme { base.copy(selectedIndex = selected.value).Content(modifier = Modifier) }
        }
        compose.onNodeWithContentDescription("Time range")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Last 14 days"))
            .performClick()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)).assertIsSelected()
        compose.onNodeWithText("Last 7 days").performClick()
        compose.runOnIdle { assertThat(selected.value).isEqualTo(0) }
        compose.onNodeWithText("Last 14 days").assertDoesNotExist()
        compose.onNodeWithContentDescription("Time range")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Last 7 days"))
    }
}
