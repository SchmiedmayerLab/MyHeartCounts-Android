//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

class MeasurementUnitsProviderTest {
    private val provider = MeasurementUnitsProviderImpl()
    private lateinit var defaultLocale: Locale

    @Before
    fun setup() {
        defaultLocale = Locale.getDefault()
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `a US locale uses feet and inches and pounds`() {
        // given
        Locale.setDefault(Locale.forLanguageTag("en-US"))

        // when
        val units = provider.currentUnits

        // then
        assertThat(units).isEqualTo(MeasurementUnits.US)
    }

    @Test
    fun `a UK locale uses centimetres and pounds`() {
        // given
        Locale.setDefault(Locale.forLanguageTag("en-GB"))

        // when
        val units = provider.currentUnits

        // then
        assertThat(units).isEqualTo(MeasurementUnits.UK)
    }

    @Test
    fun `any other locale uses centimetres and kilograms`() {
        // given
        Locale.setDefault(Locale.forLanguageTag("de-DE"))

        // when
        val units = provider.currentUnits

        // then
        assertThat(units).isEqualTo(MeasurementUnits.METRIC)
    }

    @Test
    fun `a change of the device locale applies to the next read`() {
        // given
        Locale.setDefault(Locale.forLanguageTag("de-DE"))
        val before = provider.currentUnits

        // when
        Locale.setDefault(Locale.forLanguageTag("en-US"))

        // then
        assertThat(before).isEqualTo(MeasurementUnits.METRIC)
        assertThat(provider.currentUnits).isEqualTo(MeasurementUnits.US)
    }
}
