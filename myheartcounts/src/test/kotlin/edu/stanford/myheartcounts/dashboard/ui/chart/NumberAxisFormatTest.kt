//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.chart

import com.google.common.truth.Truth.assertThat
import org.grovealliance.ui.StringResource
import org.junit.Test
import java.util.Locale

class NumberAxisFormatTest {

    private val format = NumberAxisFormat(locale = Locale.US)

    @Test
    fun `whole steps are written without decimals and grouped`() {
        // when
        val label = format.label(value = 2_000.0, step = 1_000.0)

        // then
        assertThat(label).isEqualTo(StringResource("2,000"))
    }

    @Test
    fun `half steps are written with one decimal`() {
        // when
        val labels = listOf(0.0, 0.5, 1.0).map { format.label(value = it, step = 0.5) }

        // then
        assertThat(labels).containsExactly(
            StringResource("0.0"),
            StringResource("0.5"),
            StringResource("1.0"),
        ).inOrder()
    }

    @Test
    fun `finer steps are written with more decimals`() {
        // when
        val label = format.label(value = 0.2, step = 0.1)

        // then
        assertThat(label).isEqualTo(StringResource("0.2"))
    }

    @Test
    fun `tiny steps are written with just their decimals`() {
        // when
        val labels = format.ticks(CartesianChart.Range(start = 0.0, end = 3e-5)).map { it.label }

        // then
        assertThat(labels).containsExactly(
            StringResource("0.00000"),
            StringResource("0.00001"),
            StringResource("0.00002"),
            StringResource("0.00003"),
        ).inOrder()
    }

    @Test
    fun `ranges off a clean step are marked between their bounds`() {
        // when
        val ticks = format.ticks(CartesianChart.Range(start = 101.0, end = 199.0))

        // then
        assertThat(ticks.map { it.at }).containsExactly(120.0, 140.0, 160.0, 180.0).inOrder()
    }

    @Test
    fun `numbers follow the locale`() {
        // when
        val label = NumberAxisFormat(locale = Locale.GERMANY).label(value = 2_500.5, step = 0.5)

        // then
        assertThat(label).isEqualTo(StringResource("2.500,5"))
    }
}
