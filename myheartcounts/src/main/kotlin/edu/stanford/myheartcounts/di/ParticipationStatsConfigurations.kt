//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.di

import edu.stanford.myheartcounts.achievements.AchievementRowMapper
import edu.stanford.myheartcounts.achievements.AchievementRowMapperImpl
import edu.stanford.myheartcounts.achievements.AchievementsContentMapper
import edu.stanford.myheartcounts.achievements.AchievementsContentMapperImpl
import edu.stanford.myheartcounts.achievements.AchievementsSource
import edu.stanford.myheartcounts.achievements.SampleAchievementsSource
import edu.stanford.myheartcounts.stats.ParticipationStatsContentMapper
import edu.stanford.myheartcounts.stats.SampleParticipationStatsContentMapper
import org.grovealliance.core.ConfigurationBuilder

/**
 * Registers the participation stats and achievements services.
 */
fun ConfigurationBuilder.participationStatsConfigurations() {
    singleton<AchievementsSource> { SampleAchievementsSource() }
    singleton<AchievementRowMapper> { AchievementRowMapperImpl() }
    singleton<AchievementsContentMapper> {
        AchievementsContentMapperImpl(
            source = dependency(),
            rowMapper = dependency(),
        )
    }
    singleton<ParticipationStatsContentMapper> {
        SampleParticipationStatsContentMapper(
            achievements = dependency(),
            rowMapper = dependency(),
            achievementsMapper = dependency(),
        )
    }
}
