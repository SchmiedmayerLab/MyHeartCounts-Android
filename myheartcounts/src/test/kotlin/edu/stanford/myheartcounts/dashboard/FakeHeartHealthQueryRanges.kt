//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.grovealliance.scheduler.InstantRange
import java.time.Instant

class FakeHeartHealthQueryRanges : HeartHealthQueryRanges {
    private val notifications = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    private var exerciseRange = range(start = "2025-12-28T00:00:00Z", end = "2026-01-04T00:00:00Z")
    override val changes: Flow<Unit> = notifications
    override val exercise: InstantRange get() = exerciseRange
    override val fortnight = range(start = "2025-12-22T00:00:00Z", end = "2026-01-05T00:00:00Z")
    override val custom = range(start = "2025-12-01T00:00:00Z", end = "2026-02-01T00:00:00Z")
    override val quarter = range(start = "2025-11-01T00:00:00Z", end = "2026-02-01T00:00:00Z")

    fun setExerciseRange(value: InstantRange) {
        exerciseRange = value
        notifications.tryEmit(Unit)
    }

    private fun range(start: String, end: String) = InstantRange(
        start = Instant.parse(start),
        endExclusive = Instant.parse(end),
    )
}
