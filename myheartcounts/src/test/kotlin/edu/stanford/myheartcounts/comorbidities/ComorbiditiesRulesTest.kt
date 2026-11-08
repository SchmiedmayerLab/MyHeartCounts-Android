//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.comorbidities

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.model.demographics.Comorbidities
import org.junit.Test

class ComorbiditiesRulesTest {
    private val rules = ComorbiditiesRules()

    @Test
    fun `toggling chooses a condition without a date and unchooses a chosen one`() {
        // given
        val current = Comorbidities(entries = mapOf("diabetes" to "2020"))

        // when
        val chosen = rules.toggled(
            current = current,
            condition = "heartFailure",
        )
        val unchosen = rules.toggled(
            current = current,
            condition = "diabetes",
        )

        // then
        assertThat(chosen).isEqualTo(Comorbidities(entries = mapOf("diabetes" to "2020", "heartFailure" to "")))
        assertThat(unchosen).isEqualTo(Comorbidities(entries = emptyMap()))
    }

    @Test
    fun `toggling without an answer chooses the condition`() {
        // when
        val chosen = rules.toggled(
            current = null,
            condition = "diabetes",
        )

        // then
        assertThat(chosen).isEqualTo(Comorbidities(entries = mapOf("diabetes" to "")))
    }

    @Test
    fun `a diagnosis date replaces the condition's date`() {
        // when
        val diagnosed = rules.diagnosed(
            current = Comorbidities(entries = mapOf("diabetes" to "", "heartFailure" to "2019")),
            condition = "diabetes",
            date = "2020-03",
        )

        // then
        assertThat(diagnosed).isEqualTo(Comorbidities(entries = mapOf("diabetes" to "2020-03", "heartFailure" to "2019")))
    }

    @Test
    fun `none is reported only by an empty answer and offered only without a chosen condition`() {
        // given
        val none = Comorbidities(entries = emptyMap())
        val chosen = Comorbidities(entries = mapOf("diabetes" to ""))

        // when / then
        assertThat(rules.isNone(current = null)).isFalse()
        assertThat(rules.isNone(current = none)).isTrue()
        assertThat(rules.isNone(current = chosen)).isFalse()
        assertThat(rules.offersNone(current = null)).isTrue()
        assertThat(rules.offersNone(current = none)).isTrue()
        assertThat(rules.offersNone(current = chosen)).isFalse()
    }
}
