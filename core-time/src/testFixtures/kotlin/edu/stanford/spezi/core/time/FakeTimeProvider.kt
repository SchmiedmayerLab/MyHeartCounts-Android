//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.core.time

import edu.stanford.spezi.foundation.fixtures.InstantFixtures
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * A [TimeProvider] with a controllable clock for tests.
 *
 * The reported instant starts at a fixed default and is moved with [setNow] / [advanceBy]; the zone
 * used to derive local values is set with [setZone].
 */
class FakeTimeProvider : TimeProvider {

    private var now: Instant = InstantFixtures.reference
    private var zone: ZoneId = ZoneId.systemDefault()

    /**
     * Sets the current instant to [instant].
     */
    fun setNow(instant: Instant) {
        now = instant
    }

    /**
     * Sets the zone in which local values are derived to [zone].
     */
    fun setZone(zone: ZoneId) {
        this.zone = zone
    }

    /**
     * Advances the current instant by [duration].
     */
    fun advanceBy(duration: Duration) {
        now = now.plus(duration)
    }

    override fun currentTimeMillis(): Long = now.toEpochMilli()
    override fun nowInstant(): Instant = now
    override fun nowLocalTime(): LocalTime = now.atZone(zone).toLocalTime()
    override fun nowLocalDate(): LocalDate = now.atZone(zone).toLocalDate()
    override fun nowZonedDateTime(): ZonedDateTime = now.atZone(zone)
    override fun currentOffset(): ZoneOffset = zone.rules.getOffset(now)
    override fun currentZone(): ZoneId = zone
}
