//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

/**
 * Source identity and platform-specific choices for producing statistics.
 * A null [sleepDataOrigin] uses Health Connect's full set of sleep origins.
 */
data class HealthStatsSyncConfiguration(
    val sourceId: String,
    val deriveExerciseTime: Boolean,
    val deriveBodyMassIndex: Boolean,
    val sleepDataOrigin: String?,
)
