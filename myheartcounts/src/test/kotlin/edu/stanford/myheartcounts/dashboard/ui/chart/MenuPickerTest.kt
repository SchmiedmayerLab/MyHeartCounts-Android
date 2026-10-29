//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.chart

import org.grovealliance.ui.StringResource
import org.junit.Assert.assertThrows
import org.junit.Test

class MenuPickerTest {
    private val base = MenuPicker(
        label = StringResource("Time range"),
        options = listOf(StringResource("Last 7 days")),
        selectedIndex = 0,
        onSelected = {},
    )

    @Test
    fun `empty options are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { base.copy(options = emptyList()) }
    }

    @Test
    fun `selection outside options is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { base.copy(selectedIndex = -1) }
        assertThrows(IllegalArgumentException::class.java) { base.copy(selectedIndex = 1) }
    }
}
