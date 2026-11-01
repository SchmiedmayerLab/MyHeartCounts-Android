//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import edu.stanford.myheartcounts.account.DateOfEnrollmentKey
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountIdKey
import org.grovealliance.scheduler.InstantRange
import java.time.Instant
import java.time.YearMonth

object HealthStatsWritingFixtures {
    val NOW: Instant = Instant.parse("2026-01-04T12:00:00Z")
    val MONTH: YearMonth = YearMonth.of(2026, 1)

    fun configuration() = HealthStatsSyncConfiguration(
        sourceId = "test.health.connect",
        deriveExerciseTime = true,
        deriveBodyMassIndex = true,
        sleepDataOrigin = null,
    )

    fun month() = HealthStatsMonth(
        month = MONTH,
        timeRange = InstantRange(
            start = Instant.parse("2026-01-01T00:00:00Z"),
            endExclusive = Instant.parse("2026-02-01T00:00:00Z"),
        ),
    )

    fun account(id: String = "participant") = AccountDetails().apply {
        this[AccountIdKey::class] = id
        this[DateOfEnrollmentKey::class] = NOW.minusSeconds(86400)
    }

    fun checkpoint(token: String = "next", ids: Set<String> = emptySet()) = HealthStatsCheckpoint(
        tokens = mapOf("StepsRecord" to token),
        recordIds = ids,
        zoneId = "UTC",
    )

    fun contribution(entries: List<HealthStatsEntry>) = HealthStatsContribution(
        metricId = "steps",
        month = MONTH,
        collection = HealthStatsEntryCollection.Hourly,
        sourceId = "test.health.connect",
        entries = entries,
    )

    fun snapshot(entries: List<HealthStatsEntry>, ids: Set<String>) = HealthStatsCollectedMonth(
        entries = entries,
        recordIds = ids,
    )

    fun steps() = StepsRecord(
        startTime = NOW.minusSeconds(3600),
        startZoneOffset = null,
        endTime = NOW,
        endZoneOffset = null,
        count = 100,
        metadata = Metadata.manualEntry(),
    )

    fun workout(type: Int) = ExerciseSessionRecord(
        startTime = NOW.minusSeconds(3600),
        startZoneOffset = null,
        endTime = NOW,
        endZoneOffset = null,
        exerciseType = type,
        metadata = Metadata.manualEntry(),
        segments = listOf(ExerciseSegment(
            startTime = NOW.minusSeconds(1800),
            endTime = NOW.minusSeconds(1500),
            segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE,
        )),
    )
}
