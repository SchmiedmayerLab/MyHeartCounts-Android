//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.home

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.ui.ExternalLink
import edu.stanford.spezi.testing.screenshot.ScreenshotTest
import edu.stanford.spezi.ui.AsyncTextButton
import edu.stanford.spezi.ui.ImageResource
import edu.stanford.spezi.ui.StaticSpeziScaffold
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.scheduler.MissedTasksRow
import edu.stanford.spezi.ui.scheduler.NoTasksContent
import edu.stanford.spezi.ui.scheduler.NudgeBanner
import edu.stanford.spezi.ui.scheduler.PromptedActionsCard
import edu.stanford.spezi.ui.scheduler.TaskListHeader
import edu.stanford.spezi.ui.scheduler.TaskListLayout
import edu.stanford.spezi.ui.scheduler.TaskSection
import edu.stanford.spezi.ui.scheduler.TaskTile
import edu.stanford.spezi.ui.scheduler.TaskTileHeader
import edu.stanford.spezi.ui.speziAppBar
import org.junit.Test

class HomeContentLayoutScreenshotTest : ScreenshotTest() {

    @Test
    fun `HomeContentLayout screenshot`() {
        screenshot {
            Scaffold {
                HomeContentLayout(
                    nudge = null,
                    promptedActions = null,
                    tasks = TaskListLayout(
                        header = TaskListHeader(title = StringResource(MHCStrings.home_todays_tasks)),
                        sections = listOf(
                            TaskSection(
                                title = null,
                                tiles = listOf(questionnaireTile, articleTile, completedTile),
                            )
                        ),
                    ),
                    missedTasks = missedTasksRow,
                    learnMore = learnMore,
                ).Content(modifier = Modifier.fillMaxSize())
            }
        }
    }

    @Test
    fun `HomeContentLayout with nudge and prompted actions screenshot`() {
        screenshot {
            Scaffold {
                HomeContentLayout(
                    nudge = NudgeBanner(
                        title = StringResource("Keep moving"),
                        message = StringResource("You are close to your step goal for today."),
                    ),
                    promptedActions = PromptedActionsCard(
                        icons = listOf(ImageResource(image = Icons.Default.CheckCircle)),
                        title = StringResource(MHCStrings.home_prompted_actions_title),
                        subtitle = StringResource("1 step remaining"),
                        onClick = {},
                    ),
                    tasks = TaskListLayout(
                        header = TaskListHeader(title = StringResource(MHCStrings.home_todays_tasks)),
                        sections = listOf(
                            TaskSection(
                                title = null,
                                tiles = listOf(questionnaireTile),
                            )
                        ),
                    ),
                    missedTasks = missedTasksRow,
                    learnMore = learnMore,
                ).Content(modifier = Modifier.fillMaxSize())
            }
        }
    }

    @Test
    fun `HomeContentLayout without tasks screenshot`() {
        screenshot {
            Scaffold {
                HomeContentLayout(
                    nudge = null,
                    promptedActions = null,
                    tasks = TaskListLayout(
                        header = TaskListHeader(title = StringResource(MHCStrings.home_todays_tasks)),
                        emptyState = NoTasksContent(
                            icon = ImageResource(image = Icons.Default.CheckCircle),
                            title = StringResource(MHCStrings.home_no_tasks_title),
                        ),
                        additionalSections = listOf(alwaysAvailableSection),
                    ),
                    missedTasks = null,
                    learnMore = learnMore,
                ).Content(modifier = Modifier.fillMaxSize())
            }
        }
    }

    @Composable
    private fun Scaffold(content: @Composable BoxScope.() -> Unit) {
        StaticSpeziScaffold(
            appBar = speziAppBar { title("My Heart Counts") },
            content = content,
        )
    }

    private val questionnaireTile = TaskTile(
        header = TaskTileHeader(
            icon = ImageResource(image = Icons.AutoMirrored.Filled.Assignment),
            title = StringResource("Diet"),
            categoryLabel = StringResource(MHCStrings.task_category_questionnaire),
            timing = StringResource("9:00 AM"),
        ),
        instructions = StringResource("A general diet assessment"),
        action = AsyncTextButton(
            title = StringResource(MHCStrings.task_action_answer_survey),
            action = {},
        ),
    )

    private val articleTile = TaskTile(
        header = TaskTileHeader(
            icon = ImageResource(image = Icons.Default.Description),
            title = StringResource("Welcome to My Heart Counts"),
            categoryLabel = StringResource(MHCStrings.task_category_informational),
            timing = StringResource("12:00 PM"),
        ),
        instructions = StringResource("Learn about the study"),
        action = AsyncTextButton(
            title = StringResource(MHCStrings.task_action_read_article),
            action = {},
        ),
    )

    /**
     * A finished questionnaire, which offers no action once it is done.
     */
    private val completedTile = TaskTile(
        header = TaskTileHeader(
            icon = ImageResource(image = Icons.AutoMirrored.Filled.Assignment),
            title = StringResource("Sleep Quality"),
            categoryLabel = StringResource(MHCStrings.task_category_questionnaire),
            timing = StringResource("8:00 AM"),
        ),
        instructions = StringResource("How well did you sleep last night?"),
        isCompleted = true,
    )

    /**
     * The tasks offered when nothing is scheduled for today.
     */
    private val alwaysAvailableSection = TaskSection(
        title = StringResource(MHCStrings.home_other_tasks_title),
        subtitle = StringResource(MHCStrings.home_other_tasks_subtitle),
        tiles = listOf(
            alwaysAvailableTile(
                icon = ImageResource(image = Icons.Default.MonitorHeart),
                title = StringResource(MHCStrings.always_available_ecg_title),
                categoryLabel = StringResource(MHCStrings.always_available_ecg_subtitle),
                actionTitle = StringResource(MHCStrings.task_action_take_ecg),
            ),
            alwaysAvailableTile(
                icon = ImageResource(image = Icons.Default.DirectionsWalk),
                title = StringResource(MHCStrings.always_available_six_minute_walk_title),
                categoryLabel = null,
                actionTitle = StringResource(MHCStrings.task_action_take_test),
            ),
            alwaysAvailableTile(
                icon = ImageResource(image = Icons.Default.DirectionsRun),
                title = StringResource(MHCStrings.always_available_twelve_minute_run_title),
                categoryLabel = null,
                actionTitle = StringResource(MHCStrings.task_action_take_test),
            ),
        ),
    )

    private fun alwaysAvailableTile(
        icon: ImageResource,
        title: StringResource,
        categoryLabel: StringResource?,
        actionTitle: StringResource,
    ) = TaskTile(
        header = TaskTileHeader(
            icon = icon,
            title = title,
            categoryLabel = categoryLabel,
        ),
        action = AsyncTextButton(title = actionTitle, action = {}),
    )

    private val missedTasksRow = MissedTasksRow(
        icon = ImageResource(image = Icons.Default.CalendarMonth),
        title = StringResource(MHCStrings.home_missed_tasks_title),
        subtitle = StringResource("3 missed tasks in the past 2 weeks"),
        onClick = {},
    )

    private val learnMore = ExternalLink(
        text = StringResource(MHCStrings.home_learn_more),
        onClicked = {},
    )
}
