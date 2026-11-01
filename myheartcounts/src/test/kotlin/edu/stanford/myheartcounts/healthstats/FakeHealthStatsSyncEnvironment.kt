//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

class FakeHealthStatsSyncEnvironment : HealthStatsSyncEnvironment {
    private var readable = true
    override val zoneId: String = "UTC"

    fun setReadable(value: Boolean) {
        readable = value
    }

    override suspend fun canRead(metric: HealthStatsSyncMetric, month: HealthStatsMonth): Boolean = readable
}
