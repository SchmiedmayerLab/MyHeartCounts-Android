//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import java.time.Instant

class FakeHealthStatsHistory : HealthStatsHistory {
    private var maintainedMonths = listOf(HealthStatsWritingFixtures.month())
    private var additionalMonth = HealthStatsWritingFixtures.month()

    fun setMonths(months: List<HealthStatsMonth>) {
        maintainedMonths = months
    }

    fun setAdditionalMonth(month: HealthStatsMonth) {
        additionalMonth = month
    }

    override fun months(enrolledAt: Instant): List<HealthStatsMonth> = maintainedMonths

    override fun month(containing: Instant): HealthStatsMonth = additionalMonth
}
