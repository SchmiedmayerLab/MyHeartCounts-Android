//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.stats

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.dashboard.ui.CounterCard
import edu.stanford.myheartcounts.dashboard.ui.DashboardColorPalette
import edu.stanford.myheartcounts.dashboard.ui.DashboardSection
import edu.stanford.myheartcounts.dashboard.ui.DashboardSectionList
import edu.stanford.myheartcounts.dashboard.ui.IconTextCard
import edu.stanford.myheartcounts.dashboard.ui.IconTextCardRow
import edu.stanford.myheartcounts.dashboard.ui.LabeledValue
import edu.stanford.myheartcounts.dashboard.ui.ProgressBadge
import edu.stanford.myheartcounts.dashboard.ui.ProgressRing
import edu.stanford.myheartcounts.dashboard.ui.ProgressRingCard
import edu.stanford.myheartcounts.dashboard.ui.ProgressRingRow
import edu.stanford.myheartcounts.dashboard.ui.ProgressRingRowList
import edu.stanford.myheartcounts.dashboard.ui.StatTile
import edu.stanford.myheartcounts.dashboard.ui.StatTileGrid
import edu.stanford.myheartcounts.dashboard.ui.TextBadge
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.resolve
import org.junit.Test

class ParticipationStatsScreenContentScreenshotTest : ScreenshotTest() {
    private val base = ParticipationStatsScreenContent(
        scaffoldState = GroveScaffoldState(
            appBar = groveAppBar {
                close {}
                title(MHCStrings.participation_stats_title)
                action(imageResource = ImageResource(image = Icons.Outlined.Info)) {}
            },
        ),
        page = MutableStateFlow(ParticipationStatsPage.Stats),
        stats = DashboardSectionList(
            sections = listOf(
                DashboardSection(
                    title = null,
                    content = enrollmentCard(),
                    footnote = null,
                ),
                DashboardSection(
                    title = StringResource(MHCStrings.participation_stats_engagement),
                    content = StatTileGrid(
                        tiles = listOf(
                            tile(
                                icon = Icons.Default.CheckCircle,
                                color = DashboardColorPalette.red,
                                title = MHCStrings.participation_stats_tasks_done,
                                value = "1",
                            ),
                            tile(
                                icon = Icons.AutoMirrored.Filled.Assignment,
                                color = DashboardColorPalette.purple,
                                title = MHCStrings.participation_stats_surveys_answered,
                                value = "0",
                            ),
                        ),
                    ),
                    footnote = null,
                ),
                DashboardSection(
                    title = StringResource(MHCStrings.participation_stats_fun_facts),
                    content = IconTextCard(
                        rows = listOf(
                            IconTextCardRow(
                                icon = ImageResource(
                                    image = Icons.Default.Favorite,
                                    tint = { DashboardColorPalette.red.resolve() },
                                ),
                                text = StringResource("Your heart has beaten about 681K times since you enrolled."),
                            ),
                        ),
                    ),
                    footnote = null,
                ),
            ),
        ),
        explainer = ParticipationStatsExplainer(
            text = StringResource(MHCStrings.participation_stats_explainer, StringResource("January 1, 2026")),
        ),
        achievements = DashboardSectionList(
            sections = listOf(
                DashboardSection(
                    title = StringResource(MHCStrings.achievements_category_study_participation),
                    content = ProgressRingCard(
                        rows = listOf(
                            achievement(
                                icon = Icons.Default.Celebration,
                                title = MHCStrings.achievement_initial_enrollment_title,
                                description = StringResource(MHCStrings.achievement_initial_enrollment_description),
                                progress = 1f,
                            ),
                            achievement(
                                icon = Icons.Default.CalendarMonth,
                                title = MHCStrings.achievement_anniversary_paper,
                                description = StringResource("Cross 1 week of study enrollment"),
                                progress = 0f,
                            ),
                        ),
                    ),
                    footnote = null,
                ),
                DashboardSection(
                    title = StringResource(MHCStrings.achievements_category_health),
                    content = ProgressRingCard(
                        rows = listOf(
                            achievement(
                                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                                title = MHCStrings.achievement_daily_steps_30k,
                                description = StringResource("Walk 30,000 steps in a day"),
                                progress = 0.73f,
                                badge = "73%",
                            ),
                        ),
                    ),
                    footnote = null,
                ),
            ),
        ),
        actionSink = ActionSource<ParticipationStatsAction> {}.sink(),
    )

    @Test
    fun `ParticipationStatsScreenContent stats screenshot`() {
        // when / then
        screenshot {
            base.Content(modifier = Modifier.fillMaxSize())
        }
    }

    @Test
    fun `ParticipationStatsScreenContent explainer screenshot`() {
        // given
        val screen = base.copy(
            scaffoldState = GroveScaffoldState(
                appBar = groveAppBar {
                    back {}
                    title(MHCStrings.participation_stats_info)
                },
            ),
            page = MutableStateFlow(ParticipationStatsPage.Explainer),
        )

        // when / then
        screenshot {
            screen.Content(modifier = Modifier.fillMaxSize())
        }
    }

    @Test
    fun `ParticipationStatsScreenContent achievements screenshot`() {
        // given
        val screen = base.copy(
            scaffoldState = GroveScaffoldState(
                appBar = groveAppBar {
                    back {}
                    title(MHCStrings.achievements_title)
                },
            ),
            page = MutableStateFlow(ParticipationStatsPage.Achievements),
        )

        // when / then
        screenshot {
            screen.Content(modifier = Modifier.fillMaxSize())
        }
    }

    private fun enrollmentCard() = CounterCard(
        value = StringResource("6"),
        unit = StringResource(MHCStrings.participation_stats_days),
        badge = ProgressBadge(
            icon = ImageResource(
                image = Icons.Default.MilitaryTech,
                tint = { Colors.onSurface },
            ),
            label = StringResource("5 / 20"),
            progress = 0.25f,
            progressColor = DashboardColorPalette.red,
        ),
        detail = LabeledValue(
            label = StringResource(MHCStrings.participation_stats_enrolled_since),
            value = StringResource("January 1, 2026"),
        ),
        trailingRow = achievement(
            icon = Icons.Default.CalendarMonth,
            title = MHCStrings.achievement_anniversary_paper,
            description = StringResource("Cross 1 week of study enrollment"),
            progress = 0f,
        ),
        list = ProgressRingRowList(
            label = StringResource(MHCStrings.participation_stats_upcoming_achievements),
            rows = listOf(
                achievement(
                    icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                    title = MHCStrings.achievement_daily_steps_30k,
                    description = StringResource("Walk 30,000 steps in a day"),
                    progress = 0.73f,
                ),
                achievement(
                    icon = Icons.Default.Description,
                    title = MHCStrings.achievement_first_questionnaire_title,
                    description = StringResource(MHCStrings.achievement_first_questionnaire_description),
                    progress = 0f,
                ),
            ),
        ),
        onClick = {},
    )

    private fun tile(
        icon: ImageVector,
        color: ColorToken.Custom,
        title: Int,
        value: String,
    ) = StatTile(
        icon = ImageResource(
            image = icon,
            tint = { color.resolve() },
        ),
        title = StringResource(title),
        value = StatTile.Value(
            text = StringResource(value),
            unit = null,
        ),
        footnote = null,
    )

    private fun achievement(
        icon: ImageVector,
        title: Int,
        description: StringResource,
        progress: Float,
        badge: String? = null,
    ) = ProgressRingRow(
        ring = ProgressRing(
            progress = progress,
            progressColor = DashboardColorPalette.green,
            icon = ImageResource(
                image = icon,
                tint = { Colors.onSurface },
            ),
        ),
        title = StringResource(title),
        subtitle = description,
        trailingBadge = badge?.let {
            TextBadge(
                text = StringResource(it),
                color = DashboardColorPalette.green,
            )
        },
    )
}
