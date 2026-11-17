//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.standard.FakeMeasurementUnitsProvider
import edu.stanford.myheartcounts.standard.MeasurementUnits
import org.grovealliance.ui.StringResource
import org.junit.Test

class DemographicsMeasurementMapperTest {
    private val unitsProvider = FakeMeasurementUnitsProvider()
    private val mapper = DemographicsMeasurementMapperImpl(unitsProvider = unitsProvider)

    @Test
    fun `metric answers are shown and entered as stored`() {
        // when
        val height = mapper.mapHeightText(heightInCm = 172.5)
        val weight = mapper.mapWeightText(weightInKg = 70.0)
        val heightEntry = mapper.mapHeightEntry(heightInCm = 172.5)
        val weightEntry = mapper.mapWeightEntry(weightInKg = 70.0)

        // then
        assertThat(height).isEqualTo(StringResource(MHCStrings.demographics_value_cm, "172.5"))
        assertThat(weight).isEqualTo(StringResource(MHCStrings.demographics_value_kg, "70"))
        assertThat(heightEntry).containsExactly("172.5")
        assertThat(weightEntry).containsExactly("70")
    }

    @Test
    fun `a US participant's 5 ft 8 in and 150 lb are stored in centimetres and kilograms`() {
        // given
        unitsProvider.setUnits(units = MeasurementUnits.US)

        // when
        val heightInCm = mapper.mapHeightInCm(entry = listOf("5", "8"))
        val weightInKg = mapper.mapWeightInKg(entry = listOf("150"))

        // then
        assertThat(heightInCm).isWithin(TOLERANCE).of(172.72)
        assertThat(weightInKg).isWithin(TOLERANCE).of(68.0388555)
    }

    @Test
    fun `stored values are shown again in the US units`() {
        // given
        unitsProvider.setUnits(units = MeasurementUnits.US)

        // when
        val height = mapper.mapHeightText(heightInCm = 172.72)
        val weight = mapper.mapWeightText(weightInKg = 68.0388555)
        val heightEntry = mapper.mapHeightEntry(heightInCm = 172.72)
        val weightEntry = mapper.mapWeightEntry(weightInKg = 68.0388555)

        // then
        assertThat(height).isEqualTo(StringResource(MHCStrings.demographics_value_feet_inches, 5, 8))
        assertThat(weight).isEqualTo(StringResource(MHCStrings.demographics_value_lb, "150"))
        assertThat(heightEntry).containsExactly("5", "8").inOrder()
        assertThat(weightEntry).containsExactly("150")
    }

    @Test
    fun `inches round to the nearest whole inch and carry into feet`() {
        // given
        unitsProvider.setUnits(units = MeasurementUnits.US)

        // when
        val height = mapper.mapHeightText(heightInCm = 182.5)

        // then
        assertThat(height).isEqualTo(StringResource(MHCStrings.demographics_value_feet_inches, 6, 0))
    }

    @Test
    fun `an unanswered height starts from empty feet and inches`() {
        // given
        unitsProvider.setUnits(units = MeasurementUnits.US)

        // when
        val entry = mapper.mapHeightEntry(heightInCm = null)

        // then
        assertThat(entry).containsExactly("", "")
    }

    @Test
    fun `empty inches count as none`() {
        // given
        unitsProvider.setUnits(units = MeasurementUnits.US)

        // when
        val heightInCm = mapper.mapHeightInCm(entry = listOf("6", ""))

        // then
        assertThat(heightInCm).isWithin(TOLERANCE).of(182.88)
    }

    @Test
    fun `feet and inches outside their ranges are not accepted`() {
        // given
        unitsProvider.setUnits(units = MeasurementUnits.US)

        // when
        val entries = listOf(
            listOf("2", "11"),
            listOf("9", "0"),
            listOf("5", "12"),
            listOf("", "8"),
            listOf("5.5", "0"),
        ).map { mapper.mapHeightInCm(entry = it) }

        // then
        assertThat(entries).containsExactly(null, null, null, null, null)
    }

    @Test
    fun `measurements outside their ranges are not accepted`() {
        // when
        val heights = listOf("89.9", "250", "abc").map { mapper.mapHeightInCm(entry = listOf(it)) }
        val weights = listOf("24.9", "450").map { mapper.mapWeightInKg(entry = listOf(it)) }

        // then
        assertThat(heights).containsExactly(null, null, null)
        assertThat(weights).containsExactly(null, null)
    }

    @Test
    fun `decimal commas are accepted`() {
        // when
        val heightInCm = mapper.mapHeightInCm(entry = listOf("172,5"))

        // then
        assertThat(heightInCm).isEqualTo(172.5)
    }

    @Test
    fun `a UK participant enters height in centimetres and weight in pounds`() {
        // given
        unitsProvider.setUnits(units = MeasurementUnits.UK)

        // when
        val heightInCm = mapper.mapHeightInCm(entry = listOf("180"))
        val weightInKg = mapper.mapWeightInKg(entry = listOf("54"))
        val acceptedWeightInKg = mapper.mapWeightInKg(entry = listOf("160"))

        // then
        assertThat(heightInCm).isEqualTo(180.0)
        assertThat(weightInKg).isNull()
        assertThat(acceptedWeightInKg).isWithin(TOLERANCE).of(72.5747792)
    }

    @Test
    fun `stored values are shown with at most two decimals`() {
        // when
        val height = mapper.mapHeightText(heightInCm = 172.72000000000003)
        val weight = mapper.mapWeightText(weightInKg = 68.0388555)
        val weightEntry = mapper.mapWeightEntry(weightInKg = 68.0388555)

        // then
        assertThat(height).isEqualTo(StringResource(MHCStrings.demographics_value_cm, "172.72"))
        assertThat(weight).isEqualTo(StringResource(MHCStrings.demographics_value_kg, "68.04"))
        assertThat(weightEntry).containsExactly("68.04")
    }

    private companion object {
        const val TOLERANCE = 0.000001
    }
}
