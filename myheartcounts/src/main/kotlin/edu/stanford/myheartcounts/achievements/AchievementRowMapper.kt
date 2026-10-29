//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.achievements

import edu.stanford.myheartcounts.dashboard.ui.DashboardColorPalette
import edu.stanford.myheartcounts.dashboard.ui.ProgressRing
import edu.stanford.myheartcounts.dashboard.ui.ProgressRingRow
import edu.stanford.myheartcounts.dashboard.ui.TextBadge
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.resolve
import java.text.NumberFormat

/**
 * Maps an [Achievement] to its row.
 */
interface AchievementRowMapper {

    /**
     * The row showing [achievement]: a ring filled as far as its progress, its title and its
     * description, followed by the progress in percent while it is under way when [showsProgress].
     */
    fun map(achievement: Achievement, showsProgress: Boolean): ProgressRingRow
}

class AchievementRowMapperImpl : AchievementRowMapper {

    override fun map(achievement: Achievement, showsProgress: Boolean): ProgressRingRow = ProgressRingRow(
        ring = ProgressRing(
            progress = achievement.progress,
            progressColor = DashboardColorPalette.green,
            icon = ImageResource(
                image = achievement.icon,
                tint = if (achievement.isUnlocked) {
                    { DashboardColorPalette.green.resolve() }
                } else {
                    { Colors.onSurface }
                },
            ),
        ),
        title = achievement.title,
        subtitle = achievement.description,
        trailingBadge = if (showsProgress && achievement.progress > 0f && !achievement.isUnlocked) {
            TextBadge(
                text = StringResource(NumberFormat.getPercentInstance().format(achievement.progress)),
                color = DashboardColorPalette.green,
            )
        } else {
            null
        },
    )
}
