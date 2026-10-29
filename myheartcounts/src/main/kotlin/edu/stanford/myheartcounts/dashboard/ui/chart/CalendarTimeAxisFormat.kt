//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.chart

import android.icu.text.DateTimePatternGenerator
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import edu.stanford.myheartcounts.dashboard.ui.internal.CalendarLabel
import edu.stanford.myheartcounts.dashboard.ui.internal.calendarTicks
import org.grovealliance.ui.StringResource
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.WeekFields
import java.util.Date
import java.util.Locale

/**
 * A time axis marked by calendar days, weeks, fortnights, months, quarters, years, decades or
 * centuries, whichever suits the length of the axis, with localized date labels.
 *
 * The axis is marked every day up to two weeks, every week up to a month, every other week up to
 * three months, every month up to six months, every quarter up to two years, every year up to twenty
 * years, every decade up to two hundred years, and every century beyond, measured in local time.
 * Marks fall on the start of a day, a week starting on the locale's first day, a month, a quarter, a
 * year, a decade or a century. A label shows the day, adding the month and the year when they differ
 * from one mark earlier; marks months apart show the month, adding the year when it differs from one
 * mark earlier; marks years apart show the year.
 *
 * @param coordinates the conversion between instants and numeric axis coordinates
 * @param zone the time zone days, weeks and months are counted in
 * @param locale the locale the first day of the week and the labels follow
 */
data class CalendarTimeAxisFormat(
    val coordinates: TimeCoordinates,
    val zone: ZoneId,
    val locale: Locale,
) : AxisFormat {

    /**
     * The date format of each kind of label, built once from the locale's best patterns on first use.
     * Not part of equality: two formats with the same zone, locale and coordinates are equal.
     */
    private val formats: Map<CalendarLabel, SimpleDateFormat> by lazy {
        val patterns = DateTimePatternGenerator.getInstance(locale)
        CalendarLabel.entries.associateWith { label ->
            SimpleDateFormat(patterns.getBestPattern(label.skeleton), locale).apply {
                timeZone = zone.icuTimeZone
            }
        }
    }

    override fun ticks(range: CartesianChart.Range): List<AxisFormat.Tick> {
        return calendarTicks(
            start = coordinates.instant(range.start),
            end = coordinates.instant(range.end),
            zone = zone,
            firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek,
        ).map { tick ->
            val format = formats.getValue(tick.label)
            AxisFormat.Tick(
                at = coordinates.coordinate(tick.at),
                label = StringResource(synchronized(format) { format.format(Date.from(tick.at)) }),
            )
        }
    }

    /**
     * The zone in the date formats' terms, by its offset when it is fixed.
     */
    private val ZoneId.icuTimeZone: TimeZone
        get() = when (val fixed = normalized()) {
            ZoneOffset.UTC -> TimeZone.getTimeZone(GMT)
            is ZoneOffset -> TimeZone.getTimeZone(GMT + fixed.id)
            else -> TimeZone.getTimeZone(id)
        }

    private val CalendarLabel.skeleton: String
        get() = when (this) {
            CalendarLabel.Day -> "d"
            CalendarLabel.DayMonth -> "MMMd"
            CalendarLabel.DayMonthYear -> "yMMMd"
            CalendarLabel.Month -> "MMM"
            CalendarLabel.MonthYear -> "yMMM"
            CalendarLabel.Year -> "y"
        }

    private companion object {
        /**
         * The zone fixed offsets are written relative to.
         */
        const val GMT = "GMT"
    }
}
