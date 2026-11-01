//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.entry

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale

class HeartScoreEntryFormTest {
    private val date = LocalDateTime.of(2026, 1, 1, 12, 30)

    @Test
    fun `pressure requires both whole-number values within exclusive upper limits`() {
        // given
        val form = form(component = HeartScoreComponent.BloodPressure).copy(value = "120", diastolic = "80")

        // then
        assertThat(form.entry()).isInstanceOf(HeartScoreEntry.Pressure::class.java)
        for (value in listOf("", "59", "250", "120.5", "NaN", "Infinity", "120abc")) {
            assertThat(form.copy(value = value).entry()).isNull()
        }
        assertThat(form.copy(diastolic = "150").entry()).isNull()
        assertThat(form.copy(diastolic = "29").entry()).isNull()
    }

    @Test
    fun `LDL and glucose use their respective lower limits`() {
        // given
        val ldl = form(component = HeartScoreComponent.BloodLipids).copy(value = "30")
        val glucose = form(component = HeartScoreComponent.BloodGlucose).copy(value = "40")

        // then
        assertThat(ldl.entry()).isNotNull()
        assertThat(glucose.entry()).isNotNull()
        assertThat(glucose.copy(value = "39").entry()).isNull()
        assertThat(ldl.copy(value = "400").entry()).isNull()
    }

    @Test
    fun `metric height and weight calculate BMI in canonical units`() {
        // given
        val form = form(component = HeartScoreComponent.BodyMassIndex).copy(weight = "81", height = "180")

        // when
        val entry = form.entry() as HeartScoreEntry.BodyMassIndex

        // then
        assertThat(entry.value).isWithin(0.00001).of(25.0)
        assertThat(entry.weightKg).isEqualTo(81.0)
        assertThat(entry.heightMeters).isEqualTo(1.8)
        assertThat(form.copy(height = "0").entry()).isNull()
        assertThat(form.copy(weight = "450").entry()).isNull()
    }

    @Test
    fun `feet inches and pounds convert before calculating BMI`() {
        // given
        val form = form(component = HeartScoreComponent.BodyMassIndex).copy(
            weightInPounds = true,
            heightInFeet = true,
            weight = "180",
            height = "5",
            inches = "10",
        )

        // when
        val entry = form.entry() as HeartScoreEntry.BodyMassIndex

        // then
        assertThat(entry.heightMeters).isWithin(0.00001).of(1.778)
        assertThat(entry.weightKg).isWithin(0.00001).of(81.6466266)
        assertThat(entry.value).isWithin(0.001).of(25.827)
        assertThat(form.copy(inches = "12").entry()).isNull()
        assertThat(form.copy(height = "-5").entry()).isNull()
    }

    @Test
    fun `direct BMI ignores hidden weight and height fields`() {
        // given
        val form = form(component = HeartScoreComponent.BodyMassIndex).copy(directBmi = true, value = "23.5", weight = "81", height = "180")

        // when
        val entry = form.entry() as HeartScoreEntry.BodyMassIndex

        // then
        assertThat(entry.value).isEqualTo(23.5)
        assertThat(entry.heightMeters).isNull()
        assertThat(entry.weightKg).isNull()
        assertThat(form.copy(value = "0").entry()).isNull()
    }

    @Test
    fun `decimal commas are parsed using the selected locale without accepting trailing input`() {
        // given
        val form = form(component = HeartScoreComponent.BodyMassIndex).copy(locale = Locale.GERMANY, directBmi = true, value = "23,5")

        // then
        assertThat((form.entry() as HeartScoreEntry.BodyMassIndex).value).isEqualTo(23.5)
        assertThat(form.copy(value = "23,5kg").entry()).isNull()
    }

    @Test
    fun `selected dates use the participant zone and retries retain identity`() {
        // given
        val form = form(component = HeartScoreComponent.BloodGlucose).copy(value = "90", zone = ZoneOffset.ofHours(2))

        // when
        val entry = form.entry()!!

        // then
        assertThat(entry.date.toString()).isEqualTo("2026-01-01T10:30:00Z")
        assertThat(form.entry()!!.id).isEqualTo(entry.id)
    }

    @Test
    fun `LDL and glucose accept only whole numbers`() {
        // given
        val ldl = form(component = HeartScoreComponent.BloodLipids).copy(value = "110.5")
        val glucose = form(component = HeartScoreComponent.BloodGlucose).copy(value = "90.5")

        // then
        assertThat(ldl.entry()).isNull()
        assertThat(glucose.entry()).isNull()
        assertThat(glucose.acceptsWholeNumbersOnly(field = HeartScoreEntryAction.Field.Value)).isTrue()
        assertThat(ldl.acceptsWholeNumbersOnly(field = HeartScoreEntryAction.Field.Value)).isTrue()
    }

    @Test
    fun `pounds and centimeters convert before calculating BMI`() {
        // given
        val form = form(component = HeartScoreComponent.BodyMassIndex).copy(weightInPounds = true, weight = "180", height = "178")

        // when
        val entry = form.entry() as HeartScoreEntry.BodyMassIndex

        // then
        assertThat(entry.heightMeters).isWithin(0.00001).of(1.78)
        assertThat(entry.weightKg).isWithin(0.00001).of(81.6466266)
        assertThat(form.limits(field = HeartScoreEntryAction.Field.Height)).isEqualTo(
            HeartScoreEntryForm.Limits(
                min = 90.0,
                max = 250.0,
            ),
        )
    }

    @Test
    fun `imperial height is limited to three to eight feet without a metric range check`() {
        // given
        val form = form(component = HeartScoreComponent.BodyMassIndex).copy(weightInPounds = true, heightInFeet = true, weight = "180")

        // then
        assertThat(form.copy(height = "2", inches = "11").entry()).isNull()
        assertThat(form.copy(height = "3", inches = "0").entry()).isNotNull()
        assertThat(form.copy(height = "8", inches = "11").entry()).isNotNull()
        assertThat(form.copy(height = "9", inches = "0").entry()).isNull()
    }

    @Test
    fun `violated limits are reported only for numbers outside the field range`() {
        // given
        val form = form(component = HeartScoreComponent.BloodPressure)
        val systolic = HeartScoreEntryAction.Field.Value

        // then
        assertThat(form.copy(value = "250").violatedLimits(field = systolic)).isEqualTo(
            HeartScoreEntryForm.Limits(
                min = 60.0,
                max = 250.0,
            ),
        )
        assertThat(form.copy(value = "59").violatedLimits(field = systolic)).isNotNull()
        assertThat(form.copy(value = "120").violatedLimits(field = systolic)).isNull()
        assertThat(form.copy(value = "").violatedLimits(field = systolic)).isNull()
        assertThat(form.copy(value = "12a").violatedLimits(field = systolic)).isNull()
    }

    @Test
    fun `weight and height limits follow the selected units`() {
        // given
        val metric = form(component = HeartScoreComponent.BodyMassIndex)
        val imperial = metric.copy(
            weightInPounds = true,
            heightInFeet = true,
        )

        // then
        assertThat(metric.limits(field = HeartScoreEntryAction.Field.Weight)).isEqualTo(
            HeartScoreEntryForm.Limits(
                min = 25.0,
                max = 450.0,
            ),
        )
        assertThat(imperial.limits(field = HeartScoreEntryAction.Field.Weight)).isEqualTo(
            HeartScoreEntryForm.Limits(
                min = 55.0,
                max = 1000.0,
            ),
        )
        assertThat(metric.limits(field = HeartScoreEntryAction.Field.Height)).isEqualTo(
            HeartScoreEntryForm.Limits(
                min = 90.0,
                max = 250.0,
            ),
        )
        assertThat(imperial.limits(field = HeartScoreEntryAction.Field.Height)).isEqualTo(
            HeartScoreEntryForm.Limits(
                min = 3.0,
                max = 9.0,
            ),
        )
        assertThat(metric.limits(field = HeartScoreEntryAction.Field.Value)).isNull()
    }

    private fun form(component: HeartScoreComponent) = HeartScoreEntryForm(
        component = component,
        date = date,
        weightInPounds = false,
        heightInFeet = false,
        locale = Locale.US,
        zone = ZoneOffset.UTC,
    )
}
