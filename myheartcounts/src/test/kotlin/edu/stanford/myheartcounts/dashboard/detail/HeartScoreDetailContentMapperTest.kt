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
import edu.stanford.myheartcounts.dashboard.HeartScoreDateFormatterImpl
import edu.stanford.myheartcounts.dashboard.HeartScoreMeasurement
import edu.stanford.myheartcounts.dashboard.ui.ColorBandList
import edu.stanford.myheartcounts.dashboard.ui.GaugeHeader
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.markdown.MarkdownDocument
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.StringResource
import org.junit.Test
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class HeartScoreDetailContentMapperTest {

    private val bandMapper = FakeHeartScoreBandMapper()
    private val mapper = HeartScoreDetailContentMapperImpl(
        dateFormatter = HeartScoreDateFormatterImpl(
            timeProvider = FakeTimeProvider().apply {
                setZone(ZoneOffset.UTC)
                setNow(HeartScoreDetailFixtures.NOW)
            },
        ),
        bandMapper = bandMapper,
    )
    private val pushedActions = mutableListOf<HeartScoreDetailAction>()
    private val actionSink = ActionSource<HeartScoreDetailAction> { pushedActions += it }.sink<HeartScoreDetailAction>()

    @Test
    fun `splits a night's sleep into hours and minutes`() {
        // when
        val header = header(input = input(component = HeartScoreComponent.Sleep, value = 8.5))

        // then
        val parts = (header.value as GaugeHeader.Value.Measured).parts
        assertThat(parts.map { it.text }).containsExactly(StringResource("8"), StringResource("30")).inOrder()
        assertThat(parts.map { it.unit }).containsExactly(StringResource("hr"), StringResource("min")).inOrder()
    }

    @Test
    fun `leaves out the minutes of a whole number of hours`() {
        // when
        val header = header(input = input(component = HeartScoreComponent.Sleep, value = 7.0))

        // then
        val parts = (header.value as GaugeHeader.Value.Measured).parts
        assertThat(parts.map { it.unit }).containsExactly(StringResource("hr"))
    }

    @Test
    fun `shows no data for a night without sleep`() {
        // when
        val header = header(input = input(component = HeartScoreComponent.Sleep, value = 0.0))

        // then
        assertThat(header.value).isEqualTo(GaugeHeader.Value.Missing(text = StringResource(MHCStrings.heart_detail_no_data)))
    }

    @Test
    fun `shows a missing value and an empty gauge without a measurement`() {
        // when
        val header = header(input = input(component = HeartScoreComponent.Sleep, value = null))

        // then
        assertThat(header.value).isEqualTo(GaugeHeader.Value.Missing(text = StringResource(MHCStrings.heart_detail_no_data)))
        assertThat(header.gauge.progress).isNull()
        assertThat(header.label).isEqualTo(StringResource(MHCStrings.heart_detail_last_night))
    }

    @Test
    fun `labels the value by the period or kind of record it is`() {
        // given
        val expected = mapOf(
            HeartScoreComponent.ExerciseMinutes to MHCStrings.heart_detail_header_last_7_days,
            HeartScoreComponent.StepCount to MHCStrings.heart_detail_daily_steps,
            HeartScoreComponent.Sleep to MHCStrings.heart_detail_most_recent_night,
            HeartScoreComponent.Diet to MHCStrings.heart_detail_most_recent_score,
            HeartScoreComponent.NicotineExposure to MHCStrings.heart_detail_most_recent_response,
            HeartScoreComponent.MentalWellbeing to MHCStrings.heart_detail_most_recent_response,
            HeartScoreComponent.BloodLipids to MHCStrings.heart_detail_most_recent_sample,
            HeartScoreComponent.BodyMassIndex to MHCStrings.heart_detail_most_recent_sample,
        )

        // when
        val labels = expected.keys.associateWith { header(input = input(component = it, value = 7.0)).label }

        // then
        assertThat(labels).isEqualTo(expected.mapValues { StringResource(it.value) })
    }

    @Test
    fun `notes when the value was measured relative to today`() {
        // when
        val header = header(input = input(component = HeartScoreComponent.BloodLipids, value = 110.0))

        // then
        val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(HeartScoreDetailFixtures.NOW.atZone(ZoneOffset.UTC))
        assertThat(header.footnote).isEqualTo(StringResource(time))
    }

    @Test
    fun `shows both blood pressure readings as one value`() {
        // given
        val input = input(component = HeartScoreComponent.BloodPressure, value = 1.0)
        val measurement = HeartScoreMeasurement.BloodPressure(
            systolic = 120.0,
            diastolic = 80.0,
        )

        // when
        val header = header(input = input.copy(result = input.result.copy(measurement = measurement)))

        // then
        val part = (header.value as GaugeHeader.Value.Measured).parts.single()
        assertThat(part.text).isEqualTo(StringResource(MHCStrings.heart_detail_pressure_value, "120", "80"))
        assertThat(part.unit).isEqualTo(StringResource("mmHg"))
    }

    @Test
    fun `names the nicotine exposure category`() {
        // when
        val header = header(input = input(component = HeartScoreComponent.NicotineExposure, value = 1.0))

        // then
        val part = (header.value as GaugeHeader.Value.Measured).parts.single()
        assertThat(part.text).isEqualTo(StringResource(MHCStrings.heart_detail_quit_over_5_years))
        assertThat(part.unit).isNull()
    }

    @Test
    fun `orders the header, chart, bands and explanation`() {
        // given
        bandMapper.setBands(bands = ColorBandList(bands = emptyList()))
        val chart = HeartScoreDetailFixtures.chartCard()
        val input = input(component = HeartScoreComponent.Sleep, value = 8.0).copy(
            chart = chart,
            explainer = MarkdownDocument.process(text = "Sleep information"),
        )

        // when
        val sections = mapper.map(input = input).sections

        // then
        assertThat(sections).hasSize(4)
        assertThat(sections[2].title).isEqualTo(StringResource(MHCStrings.heart_detail_score_result))
        assertThat(sections[3].content).isInstanceOf(HeartScoreAboutContent::class.java)
        assertThat(sections[1].content).isEqualTo(chart)
    }

    @Test
    fun `keeps the explanation's web links in key order`() {
        // given
        val document = MarkdownDocument.process(
            text = """
                ---
                link2: https://example.org/second
                link1: https://example.com/first
                link3: javascript:alert(1)
                ---
                Learn about this component.
            """.trimIndent(),
        )

        // when
        val sections = mapper.map(input = input(component = HeartScoreComponent.Diet, value = 17.0).copy(explainer = document)).sections
        (sections.last().content as HeartScoreAboutContent).links.forEach { it.onClicked() }

        // then
        assertThat(pushedActions).containsExactly(
            HeartScoreDetailAction.LinkSelected(url = "https://example.com/first"),
            HeartScoreDetailAction.LinkSelected(url = "https://example.org/second"),
        ).inOrder()
    }

    private fun header(input: HeartScoreDetailContentInput): GaugeHeader =
        mapper.map(input = input).sections.first().content as GaugeHeader

    private fun input(component: HeartScoreComponent, value: Double?) = HeartScoreDetailContentInput(
        result = HeartScoreDetailFixtures.result(component = component, value = value),
        chart = null,
        explainer = null,
        debugSamples = null,
        actionSink = actionSink,
    )
}
