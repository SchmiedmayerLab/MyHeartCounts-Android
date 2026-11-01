//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import edu.stanford.myheartcounts.MHCStrings
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.ui.StringResource
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Describes when a score's data was measured, relative to the participant's current day.
 */
interface HeartScoreDateFormatter {

    /**
     * [instant] as a time when it lies today, as "Yesterday" when it lies yesterday, and as a date
     * before that.
     */
    fun measuredAt(instant: Instant): StringResource
}

class HeartScoreDateFormatterImpl(
    private val timeProvider: TimeProvider,
) : HeartScoreDateFormatter {

    override fun measuredAt(instant: Instant): StringResource {
        val zone = timeProvider.currentZone()
        val measured = instant.atZone(zone)
        val today = timeProvider.nowInstant().atZone(zone).toLocalDate()
        return when (measured.toLocalDate()) {
            today -> StringResource(TIME_FORMAT.format(measured))
            today.minusDays(1) -> StringResource(MHCStrings.heart_health_measured_yesterday)
            else -> StringResource(DATE_FORMAT.format(measured))
        }
    }

    private companion object {
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    }
}
