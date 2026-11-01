//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("detekt:MagicNumber")

package edu.stanford.myheartcounts.stats

import android.icu.text.CompactDecimalFormat
import android.text.format.DateFormat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.achievements.AchievementRowMapper
import edu.stanford.myheartcounts.achievements.AchievementsContentMapper
import edu.stanford.myheartcounts.achievements.AchievementsSource
import edu.stanford.myheartcounts.dashboard.ui.CounterCard
import edu.stanford.myheartcounts.dashboard.ui.DashboardColorPalette
import edu.stanford.myheartcounts.dashboard.ui.DashboardSection
import edu.stanford.myheartcounts.dashboard.ui.DashboardSectionList
import edu.stanford.myheartcounts.dashboard.ui.IconTextCard
import edu.stanford.myheartcounts.dashboard.ui.IconTextCardRow
import edu.stanford.myheartcounts.dashboard.ui.LabeledValue
import edu.stanford.myheartcounts.dashboard.ui.ProgressBadge
import edu.stanford.myheartcounts.dashboard.ui.ProgressRingRowList
import edu.stanford.myheartcounts.dashboard.ui.StatTile
import edu.stanford.myheartcounts.dashboard.ui.StatTileGrid
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.resolve
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * What the participation stats sheet is built from.
 *
 * @param scaffoldState the sheet's scaffold, app bar included
 * @param page the page on screen
 * @param actionSink receives the sheet's intents
 */
data class ParticipationStatsContentInput(
    val scaffoldState: GroveScaffoldState,
    val page: StateFlow<ParticipationStatsPage>,
    val actionSink: ActionSink<ParticipationStatsAction>,
)

/**
 * Builds the participation stats screen.
 */
interface ParticipationStatsContentMapper {

    /**
     * The sheet for [input], with every page: the stats (the enrollment card with the achievements
     * unlocked so far and those coming up, followed by engagement, health totals, health highlights
     * and fun facts), the explanation of the stats, and the list of all achievements.
     */
    fun map(input: ParticipationStatsContentInput): ParticipationStatsScreenContent
}

/**
 * A [ParticipationStatsContentMapper] showing fixed sample values, not derived from the
 * participant's data; achievements come from [achievements], listed in full by [achievementsMapper].
 */
class SampleParticipationStatsContentMapper(
    private val achievements: AchievementsSource,
    private val rowMapper: AchievementRowMapper,
    private val achievementsMapper: AchievementsContentMapper,
) : ParticipationStatsContentMapper {

    private val locale: Locale get() = Locale.getDefault()
    private val enrollmentDate = LocalDate.of(2026, 10, 20)
    private val enrollmentDateText: String
        get() = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(enrollmentDate)

    override fun map(input: ParticipationStatsContentInput) = ParticipationStatsScreenContent(
        scaffoldState = input.scaffoldState,
        page = input.page,
        stats = DashboardSectionList(sections = sections(input = input)),
        explainer = ParticipationStatsExplainer(
            text = StringResource(
                MHCStrings.participation_stats_explainer,
                StringResource(enrollmentDateText),
            ),
        ),
        achievements = achievementsMapper.map(),
        actionSink = input.actionSink,
    )

    private fun sections(input: ParticipationStatsContentInput): List<DashboardSection> = listOf(
        DashboardSection(
            title = null,
            content = enrollmentCard(input = input),
            footnote = null,
        ),
        DashboardSection(
            title = StringResource(MHCStrings.participation_stats_engagement),
            content = StatTileGrid(tiles = engagementTiles()),
            footnote = null,
        ),
        DashboardSection(
            title = StringResource(MHCStrings.participation_stats_health_totals),
            content = StatTileGrid(tiles = healthTotalTiles()),
            footnote = null,
        ),
        DashboardSection(
            title = StringResource(MHCStrings.participation_stats_health_highlights),
            content = StatTileGrid(tiles = highlightTiles()),
            footnote = null,
        ),
        DashboardSection(
            title = StringResource(MHCStrings.participation_stats_fun_facts),
            content = IconTextCard(rows = funFacts()),
            footnote = null,
        ),
    )

    private fun enrollmentCard(input: ParticipationStatsContentInput): CounterCard {
        val unlocked = achievements.groups.sumOf { group -> group.achievements.count { it.isUnlocked } }
        return CounterCard(
            value = StringResource(integer(value = 6)),
            unit = StringResource(MHCStrings.participation_stats_days),
            badge = ProgressBadge(
                icon = ImageResource(
                    image = Icons.Default.MilitaryTech,
                    contentDescription = StringResource(MHCStrings.participation_stats_achievements),
                    tint = { Colors.onSurface },
                ),
                label = StringResource("${integer(value = unlocked)} / ${integer(value = achievements.totalCount)}"),
                progress = unlocked.toFloat() / achievements.totalCount,
                progressColor = DashboardColorPalette.red,
            ),
            detail = LabeledValue(
                label = StringResource(MHCStrings.participation_stats_enrolled_since),
                value = StringResource(enrollmentDateText),
            ),
            trailingRow = rowMapper.map(
                achievement = achievements.nextEnrollmentDuration,
                showsProgress = false,
            ),
            list = ProgressRingRowList(
                label = StringResource(MHCStrings.participation_stats_upcoming_achievements),
                rows = achievements.upcoming.map { achievement ->
                    rowMapper.map(
                        achievement = achievement,
                        showsProgress = false,
                    )
                },
            ),
            onClick = { input.actionSink.push(ParticipationStatsAction.AchievementsSelected) },
        )
    }

    private fun engagementTiles() = listOf(
        tile(
            icon = Icons.Default.CheckCircle,
            color = DashboardColorPalette.red,
            title = MHCStrings.participation_stats_tasks_done,
            value = integer(value = 1),
        ),
        tile(
            icon = Icons.AutoMirrored.Filled.Assignment,
            color = DashboardColorPalette.purple,
            title = MHCStrings.participation_stats_surveys_answered,
            value = integer(value = 0),
        ),
        tile(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            color = DashboardColorPalette.brown,
            title = MHCStrings.participation_stats_articles_read,
            value = integer(value = 1),
        ),
        tile(
            icon = Icons.Default.MonitorHeart,
            color = DashboardColorPalette.red,
            title = MHCStrings.participation_stats_ecgs_recorded,
            value = integer(value = 1),
        ),
        tile(
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            color = DashboardColorPalette.blue,
            title = MHCStrings.participation_stats_walk_run_tests,
            value = integer(value = 0),
        ),
    )

    private fun healthTotalTiles() = listOf(
        tile(
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            color = DashboardColorPalette.green,
            title = MHCStrings.participation_stats_steps,
            value = compact(value = 26_635),
        ),
        tile(
            icon = Icons.Default.Favorite,
            color = DashboardColorPalette.red,
            title = MHCStrings.participation_stats_heartbeats,
            value = compact(value = 681_000),
            footnote = StringResource(MHCStrings.participation_stats_estimate),
        ),
        tile(
            icon = Icons.Default.Map,
            color = DashboardColorPalette.blue,
            title = MHCStrings.participation_stats_distance,
            value = "${integer(value = 26)} km",
        ),
        tile(
            icon = Icons.Default.LocalFireDepartment,
            color = DashboardColorPalette.orange,
            title = MHCStrings.participation_stats_active_energy,
            value = "${decimal(value = 2.4, fractionDigits = 1)} kWh",
        ),
        tile(
            icon = Icons.AutoMirrored.Filled.DirectionsRun,
            color = DashboardColorPalette.green,
            title = MHCStrings.participation_stats_exercise_time,
            value = "2h 31m",
        ),
        tile(
            icon = Icons.Default.Bed,
            color = DashboardColorPalette.indigo,
            title = MHCStrings.participation_stats_sleep,
            value = "1d 16h",
        ),
        tile(
            icon = Icons.Default.SportsGymnastics,
            color = DashboardColorPalette.teal,
            title = MHCStrings.participation_stats_workouts,
            value = integer(value = 3),
        ),
        tile(
            icon = Icons.Default.Stairs,
            color = DashboardColorPalette.cyan,
            title = MHCStrings.participation_stats_flights_climbed,
            value = integer(value = 1),
        ),
    )

    private fun highlightTiles() = listOf(
        tile(
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            color = DashboardColorPalette.green,
            title = MHCStrings.participation_stats_best_step_day,
            value = compact(value = 8_000),
            footnote = StringResource(shortDate(date = LocalDate.of(2026, 10, 24))),
        ),
        tile(
            icon = Icons.Default.Timer,
            color = DashboardColorPalette.blue,
            title = MHCStrings.participation_stats_longest_workout,
            value = "1h 34m",
            footnote = StringResource("${shortDate(date = enrollmentDate)} • Walking"),
        ),
        tile(
            icon = Icons.Default.Favorite,
            color = DashboardColorPalette.red,
            title = MHCStrings.participation_stats_max_heart_rate,
            value = integer(value = 169),
            unit = BPM,
        ),
        tile(
            icon = Icons.Default.Bed,
            color = DashboardColorPalette.red,
            title = MHCStrings.participation_stats_resting_heart_rate,
            value = integer(value = 80),
            unit = BPM,
            footnote = StringResource(MHCStrings.participation_stats_average),
        ),
    )

    private fun funFacts() = listOf(
        fact(
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            color = DashboardColorPalette.green,
            text = StringResource(
                MHCStrings.participation_stats_fun_fact_steps,
                StringResource(integer(value = 26_635)),
                StringResource("${integer(value = 26)} km"),
                StringResource(decimal(value = 9.6, fractionDigits = 1)),
            ),
        ),
        fact(
            icon = Icons.Default.Favorite,
            color = DashboardColorPalette.red,
            text = StringResource(
                MHCStrings.participation_stats_fun_fact_heartbeats,
                StringResource(compact(value = 681_000)),
                StringResource(percent(value = 0.0002)),
            ),
        ),
        fact(
            icon = Icons.Default.LocalFireDepartment,
            color = DashboardColorPalette.orange,
            text = StringResource(
                MHCStrings.participation_stats_fun_fact_active_energy,
                StringResource(integer(value = 2_053)),
                StringResource(integer(value = 7)),
            ),
        ),
        fact(
            icon = Icons.Default.Bed,
            color = DashboardColorPalette.indigo,
            text = StringResource(
                MHCStrings.participation_stats_fun_fact_sleep,
                StringResource(decimal(value = 1.7, fractionDigits = 1)),
            ),
        ),
    )

    @Suppress("detekt:LongParameterList")
    private fun tile(
        icon: ImageVector,
        color: ColorToken.Custom,
        title: Int,
        value: String,
        unit: String? = null,
        footnote: StringResource? = null,
    ) = StatTile(
        icon = ImageResource(
            image = icon,
            tint = { color.resolve() },
        ),
        title = StringResource(title),
        value = StatTile.Value(
            text = StringResource(value),
            unit = unit?.let { StringResource(it) },
        ),
        footnote = footnote,
    )

    private fun fact(icon: ImageVector, color: ColorToken.Custom, text: StringResource) = IconTextCardRow(
        icon = ImageResource(
            image = icon,
            tint = { color.resolve() },
        ),
        text = text,
    )

    private fun integer(value: Int): String = NumberFormat.getIntegerInstance(locale).format(value)

    private fun decimal(value: Double, fractionDigits: Int): String = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = fractionDigits
        maximumFractionDigits = fractionDigits
    }.format(value)

    private fun percent(value: Double): String = NumberFormat.getPercentInstance(locale).apply {
        maximumFractionDigits = 2
    }.format(value)

    private fun compact(value: Int): String =
        CompactDecimalFormat.getInstance(locale, CompactDecimalFormat.CompactStyle.SHORT).format(value)

    private fun shortDate(date: LocalDate): String =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMd"), locale).format(date)

    private companion object {
        const val BPM = "bpm"
    }
}
