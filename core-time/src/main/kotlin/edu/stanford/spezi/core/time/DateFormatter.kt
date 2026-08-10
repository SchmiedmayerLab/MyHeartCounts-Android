//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.core.time

import android.content.Context
import edu.stanford.spezi.core.DefaultInitializer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.Date

/**
 * Provides date and time formatting utilities.
 */
interface DateFormatter {

    /**
     * Formats the given [date] using the given [format].
     *
     * @param date The [TemporalAccessor] to format.
     * @param format The [DateFormat] to use for formatting.
     */
    fun <T : TemporalAccessor> format(date: T, format: DateFormat): String

    /**
     * Formats the given [instant] using the given [format] in UTC.
     *
     * @param instant The [Instant] to format.
     * @param format The [DateFormat] to use for formatting.
     */
    fun formatUTC(instant: Instant, format: DateFormat): String

    /**
     * Formats the given [instant] using the given [format] and the system default [ZoneId].
     */
    fun formatDefaultZoneId(instant: Instant, format: DateFormat): String

    /**
     * Formats the given [date] using the given [format].
     */
    fun format(date: Date, format: DateFormat): String

    /**
     * Formats the given [instant] using the given [format] and [zoneId].
     */
    fun format(instant: Instant, format: DateFormat, zoneId: ZoneId): String

    /**
     * Creates the default [DateFormatter] implementation.
     */
    companion object : DefaultInitializer<DateFormatter> {
        override fun create(context: Context): DateFormatter {
            return DateFormatterImpl()
        }
    }
}

internal class DateFormatterImpl : DateFormatter {
    override fun <T : TemporalAccessor> format(date: T, format: DateFormat): String {
        if (date is Instant) return formatDefaultZoneId(date, format)
        return DateTimeFormatter.ofPattern(format.pattern).format(date)
    }

    override fun formatUTC(instant: Instant, format: DateFormat): String {
        return format(instant, format, ZoneId.of("UTC"))
    }

    override fun formatDefaultZoneId(instant: Instant, format: DateFormat): String {
        return format(instant, format, ZoneId.systemDefault())
    }

    override fun format(date: Date, format: DateFormat): String = format(date.toInstant(), format)

    override fun format(instant: Instant, format: DateFormat, zoneId: ZoneId): String {
        return DateTimeFormatter.ofPattern(format.pattern).format(instant.atZone(zoneId))
    }
}

/**
 * Common patterns to use when formatting [TemporalAccessor]s
 */
sealed class DateFormat(open val pattern: String) {
    data object MM_DD_YYYY : DateFormat(pattern = "MM/dd/yyyy")
    data object HH_MM : DateFormat(pattern = "hh:mm")
    data class Custom(override val pattern: String) : DateFormat(pattern)
}
