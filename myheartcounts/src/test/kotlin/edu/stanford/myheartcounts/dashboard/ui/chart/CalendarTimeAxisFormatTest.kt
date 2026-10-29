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
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

class CalendarTimeAxisFormatTest {

    private val coordinates = TimeCoordinates(origin = Instant.parse("2026-10-11T00:00:00Z"))

    @Test
    fun `formats with the same settings are equal`() {
        // when
        val first = CalendarTimeAxisFormat(coordinates = coordinates, zone = ZoneOffset.UTC, locale = Locale.US)
        val second = CalendarTimeAxisFormat(coordinates = coordinates, zone = ZoneOffset.UTC, locale = Locale.US)

        // then
        assertThat(first).isEqualTo(second)
        assertThat(first.hashCode()).isEqualTo(second.hashCode())
    }

    @Test
    fun `labels follow a fixed offset zone`() {
        // given
        val zone = ZoneOffset.ofHours(2)
        val start = LocalDate.of(2026, 1, 1).atStartOfDay(zone).toInstant()
        val format = CalendarTimeAxisFormat(coordinates = coordinates, zone = zone, locale = Locale.US)

        // when
        val ticks = format.ticks(coordinates.range(start = start, end = start.plus(Duration.ofDays(2))))

        // then
        assertThat(ticks.map { it.label }).containsExactly(
            StringResource("Jan 1, 2026"),
            StringResource("2"),
        ).inOrder()
    }

    @Test
    fun `formats with different settings differ`() {
        // when
        val us = CalendarTimeAxisFormat(coordinates = coordinates, zone = ZoneOffset.UTC, locale = Locale.US)
        val germany = us.copy(locale = Locale.GERMANY)

        // then
        assertThat(us).isNotEqualTo(germany)
    }
}
