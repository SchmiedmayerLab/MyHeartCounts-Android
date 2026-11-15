//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.comorbidities

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.MHCStrings
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.ui.StringResource
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

class DiagnosisDateMapperTest {
    private val timeProvider = FakeTimeProvider()
    private val mapper = DiagnosisDateMapperImpl(
        conditionsProvider = ComorbiditiesConditionsProviderImpl(),
        timeProvider = timeProvider,
    )
    private val defaultLocale = Locale.getDefault()

    @Before
    fun setup() {
        Locale.setDefault(Locale.US)
        timeProvider.setZone(zone = ZoneOffset.UTC)
        timeProvider.setNow(instant = LocalDate.of(2026, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant())
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `dates read as month and year, the year alone, or unknown`() {
        // when / then
        assertThat(mapper.mapText(date = "2020-03")).isEqualTo(StringResource("Mar 2020"))
        assertThat(mapper.mapText(date = "2019")).isEqualTo(StringResource("2019"))
        assertThat(mapper.mapText(date = "")).isEqualTo(StringResource(MHCStrings.comorbidities_since_unknown))
    }

    @Test
    fun `the picker opens on the date and offers the past hundred years`() {
        // when
        val picker = mapper.mapPicker(
            condition = "diabetes",
            date = "2020-03",
            onChange = {},
            onDismiss = {},
        )

        // then
        assertThat(picker.title).isEqualTo(StringResource(MHCStrings.comorbidities_condition_diabetes))
        assertThat(picker.year).isEqualTo(2020)
        assertThat(picker.month).isEqualTo(3)
        assertThat(picker.years.first()).isEqualTo(2026)
        assertThat(picker.years.last()).isEqualTo(1926)
    }

    @Test
    fun `the picker reports a year and month, a year, or an unknown date`() {
        // given
        val changes = mutableListOf<String>()
        val picker = mapper.mapPicker(
            condition = "diabetes",
            date = "",
            onChange = { changes += it },
            onDismiss = {},
        )

        // when
        picker.onSelect(2021, 7)
        picker.onSelect(2019, null)
        picker.onClear()

        // then
        assertThat(changes).containsExactly("2021-07", "2019", "").inOrder()
    }
}
