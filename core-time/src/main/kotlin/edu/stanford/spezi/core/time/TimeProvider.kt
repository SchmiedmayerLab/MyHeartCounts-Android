//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.core.time

import android.content.Context
import edu.stanford.spezi.core.DefaultInitializer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Provides access to the current date and time.
 *
 * Use this abstraction instead of calling platform time APIs directly when code should be
 * configurable or testable.
 */
interface TimeProvider {
    /**
     * Returns the current time in milliseconds since the Unix epoch.
     */
    fun currentTimeMillis(): Long

    /**
     * Returns the current instant.
     */
    fun nowInstant(): Instant

    /**
     * Returns the current local time in the device's default time zone.
     */
    fun nowLocalTime(): LocalTime

    /**
     * Returns the current local date in the device's default time zone.
     */
    fun nowLocalDate(): LocalDate

    /**
     * Returns the current zoned date-time in the device's default time zone.
     */
    fun nowZonedDateTime(): ZonedDateTime

    /**
     * Returns the current zone offset for the device's default time zone.
     */
    fun currentOffset(): ZoneOffset

    /**
     * Creates the default [TimeProvider] implementation.
     */
    companion object : DefaultInitializer<TimeProvider> {
        override fun create(context: Context): TimeProvider {
            return TimeProviderImpl()
        }
    }
}

internal class TimeProviderImpl : TimeProvider {
    override fun currentTimeMillis(): Long = nowInstant().toEpochMilli()
    override fun nowInstant(): Instant = Instant.now()
    override fun nowLocalTime(): LocalTime = LocalTime.now()
    override fun nowLocalDate(): LocalDate = LocalDate.now()
    override fun nowZonedDateTime(): ZonedDateTime = ZonedDateTime.now()
    override fun currentOffset(): ZoneOffset = ZonedDateTime.now().offset
}
