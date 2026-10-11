//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class MHCEnvironmentTrackerTest {

    @Test
    fun `reports the US customary system for the United States, Liberia and Myanmar`() {
        // when
        val systems = listOf("en-US", "en-LR", "my-MM").map {
            MHCEnvironmentTracker.measurementSystem(locale = Locale.forLanguageTag(it))
        }

        // then
        assertThat(systems).containsExactly("ussystem", "ussystem", "ussystem")
    }

    @Test
    fun `reports the imperial system for the United Kingdom`() {
        // when
        val system = MHCEnvironmentTracker.measurementSystem(locale = Locale.forLanguageTag("en-GB"))

        // then
        assertThat(system).isEqualTo("uksystem")
    }

    @Test
    fun `reports the metric system everywhere else`() {
        // when
        val systems = listOf("de-DE", "es-MX", "en").map {
            MHCEnvironmentTracker.measurementSystem(locale = Locale.forLanguageTag(it))
        }

        // then
        assertThat(systems).containsExactly("metric", "metric", "metric")
    }
}
