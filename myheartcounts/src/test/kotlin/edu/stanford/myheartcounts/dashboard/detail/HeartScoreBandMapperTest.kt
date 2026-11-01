//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import edu.stanford.myheartcounts.dashboard.ui.ColorBandList
import org.grovealliance.ui.StringResource
import org.junit.Test

class HeartScoreBandMapperTest {

    private val mapper = HeartScoreBandMapperImpl(labels = HeartScoreBandLabelsImpl())

    @Test
    fun `explains mental wellbeing by its gauge alone`() {
        // when
        val bands = mapper.map(result = HeartScoreDetailFixtures.result(component = HeartScoreComponent.MentalWellbeing, value = 60.0))

        // then
        assertThat(bands).isNull()
    }

    @Test
    fun `highlights no band without a value`() {
        // given
        val components = HeartScoreComponent.entries - HeartScoreComponent.MentalWellbeing

        // when
        val highlighted = components.map { bands(component = it, value = null).count { band -> band.isHighlighted } }

        // then
        assertThat(highlighted.toSet()).containsExactly(0)
    }

    @Test
    fun `lists exercise bands best first and highlights the one holding the value`() {
        // when
        val bands = bands(component = HeartScoreComponent.ExerciseMinutes, value = 150.0)

        // then
        assertThat(bands.map { it.leadingText }).containsExactly(
            StringResource(MHCStrings.heart_detail_band_at_least_spaced, "150"),
            StringResource(MHCStrings.heart_detail_band_range, "120", "149"),
            StringResource(MHCStrings.heart_detail_band_range, "90", "119"),
            StringResource(MHCStrings.heart_detail_band_range, "60", "89"),
            StringResource(MHCStrings.heart_detail_band_range, "30", "59"),
            StringResource(MHCStrings.heart_detail_band_range, "1", "29"),
        ).inOrder()
        assertThat(bands.map { it.trailingText }).containsExactly(
            StringResource("100"),
            StringResource("90"),
            StringResource("80"),
            StringResource("60"),
            StringResource("40"),
            StringResource("20"),
        ).inOrder()
        assertThat(bands.map { it.isHighlighted }).containsExactly(true, false, false, false, false, false).inOrder()
    }

    @Test
    fun `labels body mass index bands with their weight category`() {
        // when
        val bands = bands(component = HeartScoreComponent.BodyMassIndex, value = 22.0)

        // then
        assertThat(bands[2].leadingText).isEqualTo(
            StringResource(
                MHCStrings.heart_detail_bmi_band,
                StringResource(MHCStrings.heart_detail_band_range, "18.5", "24.9"),
                StringResource(MHCStrings.heart_detail_bmi_normal),
            ),
        )
        assertThat(bands[2].isHighlighted).isTrue()
    }

    @Test
    fun `uses the Asian body mass index cut-offs when they apply`() {
        // given
        val result = HeartScoreDetailFixtures.result(component = HeartScoreComponent.BodyMassIndex, value = 24.0)

        // when
        val standard = mapper.map(result = result)!!.bands.indexOfFirst { it.isHighlighted }
        val asian = mapper.map(result = result.copy(usesAsianBodyMassIndex = true))!!.bands

        // then
        assertThat(standard).isEqualTo(2)
        assertThat(asian.indexOfFirst { it.isHighlighted }).isEqualTo(3)
        assertThat(asian[3].leadingText).isEqualTo(
            StringResource(
                MHCStrings.heart_detail_bmi_band,
                StringResource(MHCStrings.heart_detail_band_range, "23", "24.9"),
                StringResource(MHCStrings.heart_detail_bmi_at_risk),
            ),
        )
    }

    @Test
    fun `orders sleep bands by score and highlights long nights`() {
        // when
        val bands = bands(component = HeartScoreComponent.Sleep, value = 10.5)

        // then
        assertThat(bands.map { it.trailingText }).containsExactly(
            StringResource("100"),
            StringResource("90"),
            StringResource("70"),
            StringResource("40"),
            StringResource("40"),
            StringResource("20"),
        ).inOrder()
        assertThat(bands[4].leadingText).isEqualTo(StringResource(MHCStrings.heart_detail_sleep_band_at_least, "10"))
        assertThat(bands[4].isHighlighted).isTrue()
    }

    @Test
    fun `highlights no diet band for a value outside its ranges`() {
        // when
        val bands = bands(component = HeartScoreComponent.Diet, value = 22.0)

        // then
        assertThat(bands.none { it.isHighlighted }).isTrue()
    }

    @Test
    fun `names each nicotine exposure category`() {
        // when
        val bands = bands(component = HeartScoreComponent.NicotineExposure, value = 4.0)

        // then
        assertThat(bands.map { it.leadingText }).containsExactly(
            StringResource(MHCStrings.heart_detail_never_smoked),
            StringResource(MHCStrings.heart_detail_quit_over_5_years),
            StringResource(MHCStrings.heart_detail_quit_1_to_5_years),
            StringResource(MHCStrings.heart_detail_recent_quit),
            StringResource(MHCStrings.heart_detail_smoking),
        ).inOrder()
        assertThat(bands.last().isHighlighted).isTrue()
    }

    @Test
    fun `shows blood pressure bands without scores`() {
        // when
        val bands = bands(component = HeartScoreComponent.BloodPressure, value = null)

        // then
        assertThat(bands).hasSize(4)
        assertThat(bands.map { it.trailingText }.toSet()).containsExactly(null)
    }

    private fun bands(component: HeartScoreComponent, value: Double?): List<ColorBandList.Band> =
        mapper.map(result = HeartScoreDetailFixtures.result(component = component, value = value))!!.bands
}
