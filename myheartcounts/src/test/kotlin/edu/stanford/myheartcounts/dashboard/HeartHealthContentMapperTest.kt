//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.dashboard.ui.DashboardColorPalette
import edu.stanford.myheartcounts.dashboard.ui.Gauge
import edu.stanford.myheartcounts.dashboard.ui.GaugeCard
import edu.stanford.myheartcounts.dashboard.ui.GaugeTile
import edu.stanford.myheartcounts.dashboard.ui.GaugeTileGrid
import edu.stanford.myheartcounts.ui.MarkdownCard
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.markdown.MarkdownDocument
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorToken
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class HeartHealthContentMapperTest {

    private val zone: ZoneId = ZoneId.of("Europe/Zurich")
    private val now: Instant = Instant.parse("2026-01-04T10:00:00Z")
    private val mapper = HeartHealthContentMapperImpl(
        dateFormatter = HeartScoreDateFormatterImpl(
            timeProvider = FakeTimeProvider().apply {
                setZone(zone)
                setNow(now)
            },
        ),
    )
    private val pushedActions = mutableListOf<HeartHealthAction>()
    private val actionSink = ActionSource<HeartHealthAction> { pushedActions.add(it) }.sink<HeartHealthAction>()

    @Test
    fun `shows the overall score out of a hundred on a colored gauge`() {
        // when
        val gauge = overallGauge(scores(overall = 0.7))

        // then
        assertThat(gauge.progress).isWithin(TOLERANCE).of(0.7f)
        assertThat(gauge.colors).isEqualTo(DashboardColorPalette.redToGreen)
        assertThat(gauge.label?.value).isEqualTo(StringResource("70"))
        assertThat(gauge.size).isEqualTo(Gauge.Size.Large)
    }

    @Test
    fun `drops the fraction of the overall score rather than rounding it`() {
        // when
        val gauge = overallGauge(scores(overall = 0.879))

        // then
        assertThat(gauge.label?.value).isEqualTo(StringResource("87"))
    }

    @Test
    fun `shows a muted dash while there is no overall score`() {
        // when
        val gauge = overallGauge(scores(overall = null))

        // then
        assertThat(gauge.progress).isNull()
        assertThat(gauge.colors).containsExactly(ColorToken.Outline)
        assertThat(gauge.label?.value).isEqualTo(StringResource(MHCStrings.heart_health_no_score))
    }

    @Test
    fun `lists a tile per factor in order`() {
        // when
        val tiles = tiles(scores(overall = null))

        // then
        assertThat(tiles.map { it.title }).containsExactlyElementsIn(
            HeartHealthFixtures.SHOWN_COMPONENTS.map { StringResource(it.title) },
        ).inOrder()
    }

    @Test
    fun `notes the time of a factor measured today`() {
        // given
        val measuredAt = now.minus(Duration.ofHours(2))

        // when
        val caption = sleepCaption(measuredAt = measuredAt)

        // then
        val expected = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(measuredAt.atZone(zone))
        assertThat(caption).isEqualTo(StringResource(expected))
    }

    @Test
    fun `notes a factor measured yesterday as such`() {
        // when
        val caption = sleepCaption(measuredAt = now.minus(Duration.ofDays(1)))

        // then
        assertThat(caption).isEqualTo(StringResource(MHCStrings.heart_health_measured_yesterday))
    }

    @Test
    fun `notes the date of a factor measured earlier`() {
        // given
        val measuredAt = now.minus(Duration.ofDays(4))

        // when
        val caption = sleepCaption(measuredAt = measuredAt)

        // then
        val expected = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(measuredAt.atZone(zone))
        assertThat(caption).isEqualTo(StringResource(expected))
    }

    @Test
    fun `invites the participant to learn more about an unscored factor`() {
        // when
        val tile = tiles(scores(overall = null)).first()

        // then
        assertThat(tile.gauge.progress).isNull()
        assertThat(tile.gauge.label?.caption)
            .isEqualTo(Gauge.Caption.Description(text = StringResource(MHCStrings.heart_health_learn_more)))
    }

    @Test
    fun `opens a factor's details when its tile is tapped`() {
        // given
        val tiles = tiles(scores(overall = null))

        // when
        tiles[1].onClick?.invoke()

        // then
        assertThat(pushedActions).containsExactly(
            HeartHealthAction.ComponentSelected(component = HeartHealthFixtures.SHOWN_COMPONENTS[1]),
        )
    }

    @Test
    fun `explains the score below the factors when the study provides an explanation`() {
        // given
        val explainer = MarkdownDocument.process(text = "#### What is this score?")

        // when
        val section = map(scores = scores(overall = null), explainer = explainer).sections.singleOrNull { it.content is MarkdownCard }

        // then
        assertThat(section?.title).isEqualTo(StringResource(MHCStrings.heart_health_explainer_title))
        assertThat(section?.content).isEqualTo(MarkdownCard(document = explainer))
    }

    @Test
    fun `leaves out the explanation when the study provides none`() {
        // when
        val layout = map(scores = scores(overall = null), explainer = null)

        // then
        assertThat(layout.sections.none { it.content is MarkdownCard }).isTrue()
    }

    private fun scores(overall: Double?) = HeartHealthFixtures.scores(
        overall = overall,
        scored = emptySet(),
        score = 0.0,
        measuredAt = now,
    )

    private fun sleepCaption(measuredAt: Instant): StringResource? {
        val scores = HeartHealthFixtures.scores(
            overall = null,
            scored = setOf(HeartScoreComponent.Sleep),
            score = 1.0,
            measuredAt = measuredAt,
        )
        val sleep = tiles(scores).single { it.title == StringResource(HeartScoreComponent.Sleep.title) }
        return (sleep.gauge.label?.caption as Gauge.Caption.Description).text
    }

    private fun map(
        scores: HeartHealthScores,
        explainer: MarkdownDocument? = null,
    ): HeartHealthContentLayout = mapper.map(
        input = HeartHealthContentInput(
            scores = scores,
            explainer = explainer,
            actionSink = actionSink,
        ),
    )

    private fun overallGauge(scores: HeartHealthScores): Gauge = (map(scores).sections.first().content as GaugeCard).gauge

    private fun tiles(scores: HeartHealthScores): List<GaugeTile> = (map(scores).sections[1].content as GaugeTileGrid).tiles

    private companion object {
        const val TOLERANCE = 1e-6f
    }
}
