//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("detekt:MagicNumber")

package edu.stanford.myheartcounts.achievements

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MonitorHeart
import edu.stanford.myheartcounts.MHCStrings
import org.grovealliance.ui.StringResource
import java.text.NumberFormat

/**
 * The achievements shown to the participant.
 */
interface AchievementsSource {

    /**
     * Every achievement on display, grouped by category in the order they are shown. A progression
     * shows the levels already unlocked and its next locked level only.
     */
    val groups: List<AchievementGroup>

    /**
     * The next level of the study enrollment progression.
     */
    val nextEnrollmentDuration: Achievement

    /**
     * The locked achievements closest to being unlocked, apart from [nextEnrollmentDuration].
     */
    val upcoming: List<Achievement>

    /**
     * How many achievements the participant can unlock in all, locked ones hidden from [groups]
     * included.
     */
    val totalCount: Int
}

/**
 * An [AchievementsSource] serving a fixed set of sample achievements, not derived from the
 * participant's data.
 */
class SampleAchievementsSource : AchievementsSource {

    private val steps = NumberFormat.getIntegerInstance()

    private val firstQuestionnaire = Achievement(
        icon = Icons.Default.Description,
        title = StringResource(MHCStrings.achievement_first_questionnaire_title),
        description = StringResource(MHCStrings.achievement_first_questionnaire_description),
        progress = 0f,
    )
    private val firstWalkTest = Achievement(
        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
        title = StringResource(MHCStrings.achievement_first_walk_test_title),
        description = StringResource(MHCStrings.achievement_first_walk_test_description),
        progress = 0f,
    )
    private val firstRunTest = Achievement(
        icon = Icons.AutoMirrored.Filled.DirectionsRun,
        title = StringResource(MHCStrings.achievement_first_run_test_title),
        description = StringResource(MHCStrings.achievement_first_run_test_description),
        progress = 0f,
    )
    private val firstEcg = Achievement(
        icon = Icons.Default.MonitorHeart,
        title = StringResource(MHCStrings.achievement_first_ecg_title),
        description = StringResource(MHCStrings.achievement_first_ecg_description),
        progress = 1f,
    )
    private val initialEnrollment = Achievement(
        icon = Icons.Default.Celebration,
        title = StringResource(MHCStrings.achievement_initial_enrollment_title),
        description = StringResource(MHCStrings.achievement_initial_enrollment_description),
        progress = 1f,
    )

    override val nextEnrollmentDuration = Achievement(
        icon = Icons.Default.CalendarMonth,
        title = StringResource(MHCStrings.achievement_anniversary_paper),
        description = StringResource(
            MHCStrings.achievement_enrollment_duration_description,
            StringResource(MHCStrings.achievement_enrollment_1_week),
        ),
        progress = 0f,
    )

    private val nextDailySteps = dailySteps(
        title = MHCStrings.achievement_daily_steps_30k,
        target = 30_000,
        progress = 0.73f,
    )

    override val groups = listOf(
        AchievementGroup(
            title = StringResource(MHCStrings.achievements_category_study_participation),
            achievements = listOf(
                initialEnrollment,
                nextEnrollmentDuration,
            ),
        ),
        AchievementGroup(
            title = StringResource(MHCStrings.achievements_category_general),
            achievements = listOf(
                firstQuestionnaire,
                firstWalkTest,
                firstRunTest,
                firstEcg,
            ),
        ),
        AchievementGroup(
            title = StringResource(MHCStrings.achievements_category_health),
            achievements = listOf(
                dailySteps(
                    title = MHCStrings.achievement_daily_steps_10k,
                    target = 10_000,
                    progress = 1f,
                ),
                dailySteps(
                    title = MHCStrings.achievement_daily_steps_15k,
                    target = 15_000,
                    progress = 1f,
                ),
                dailySteps(
                    title = MHCStrings.achievement_daily_steps_20k,
                    target = 20_000,
                    progress = 1f,
                ),
                nextDailySteps,
            ),
        ),
    )

    override val upcoming = listOf(
        nextDailySteps,
        firstQuestionnaire,
        firstWalkTest,
    )

    override val totalCount = 20

    private fun dailySteps(title: Int, target: Int, progress: Float) = Achievement(
        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
        title = StringResource(title),
        description = StringResource(MHCStrings.achievement_daily_steps_description, StringResource(steps.format(target))),
        progress = progress,
    )
}
