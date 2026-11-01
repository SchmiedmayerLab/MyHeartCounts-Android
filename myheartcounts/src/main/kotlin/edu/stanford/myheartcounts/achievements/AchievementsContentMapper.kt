//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.achievements

import edu.stanford.myheartcounts.dashboard.ui.DashboardSection
import edu.stanford.myheartcounts.dashboard.ui.DashboardSectionList
import edu.stanford.myheartcounts.dashboard.ui.ProgressRingCard

/**
 * Builds the list of all achievements.
 */
interface AchievementsContentMapper {

    /**
     * One card per category of achievements, each achievement showing its progress in percent while
     * it is under way.
     */
    fun map(): DashboardSectionList
}

/**
 * Default [AchievementsContentMapper], showing the achievements [source] serves.
 */
class AchievementsContentMapperImpl(
    private val source: AchievementsSource,
    private val rowMapper: AchievementRowMapper,
) : AchievementsContentMapper {

    override fun map() = DashboardSectionList(
        sections = source.groups.map { group ->
            DashboardSection(
                title = group.title,
                content = ProgressRingCard(
                    rows = group.achievements.map { achievement ->
                        rowMapper.map(
                            achievement = achievement,
                            showsProgress = true,
                        )
                    },
                ),
                footnote = null,
            )
        },
    )
}
