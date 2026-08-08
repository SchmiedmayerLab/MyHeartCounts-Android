//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.scheduler.internal

import edu.stanford.spezi.core.time.TimeProvider
import edu.stanford.spezi.scheduler.AllowedCompletionPolicy
import edu.stanford.spezi.scheduler.AllowedCompletionPolicy.AFTER_START
import edu.stanford.spezi.scheduler.AllowedCompletionPolicy.ANYTIME
import edu.stanford.spezi.scheduler.AllowedCompletionPolicy.DURING_EVENT
import edu.stanford.spezi.scheduler.AllowedCompletionPolicy.SAME_DAY
import edu.stanford.spezi.scheduler.AllowedCompletionPolicy.SAME_DAY_AFTER_START
import edu.stanford.spezi.scheduler.Occurrence
import java.time.Instant

/**
 * Applies an [AllowedCompletionPolicy] to an occurrence, resolving the current time and calendar-day
 * boundaries against [timeProvider].
 */
internal class CompletionPolicyEvaluator(private val timeProvider: TimeProvider) {

    /**
     * Whether [policy] permits completing [occurrence] now.
     */
    fun isAllowedToComplete(policy: AllowedCompletionPolicy, occurrence: Occurrence): Boolean {
        val at = timeProvider.nowInstant()
        val zone = timeProvider.currentZone()
        fun isToday(instant: Instant) = instant.atZone(zone).toLocalDate() == at.atZone(zone).toLocalDate()
        return when (policy) {
            SAME_DAY -> isToday(occurrence.start)
            AFTER_START -> at >= occurrence.start
            SAME_DAY_AFTER_START -> isToday(occurrence.start) && at >= occurrence.start
            DURING_EVENT -> at >= occurrence.start && at < occurrence.end
            ANYTIME -> true
        }
    }

    /**
     * The instant at which [policy] starts permitting completion of [occurrence]; `null` if already
     * allowed or never becoming allowed. [ANYTIME] returns [Instant.MIN].
     */
    fun dateOnceCompletionIsAllowed(policy: AllowedCompletionPolicy, occurrence: Occurrence): Instant? {
        val now = timeProvider.nowInstant()
        val zone = timeProvider.currentZone()
        val allowedFrom = when (policy) {
            SAME_DAY -> occurrence.start.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
            AFTER_START, SAME_DAY_AFTER_START, DURING_EVENT -> occurrence.start
            ANYTIME -> return Instant.MIN
        }
        return if (now < allowedFrom) allowedFrom else null
    }

    /**
     * The instant at which [policy] stops permitting completion of [occurrence]; `null` if it does
     * not become disallowed in the future. [ANYTIME] returns [Instant.MAX].
     */
    fun dateOnceCompletionBecomesDisallowed(policy: AllowedCompletionPolicy, occurrence: Occurrence): Instant? =
        when (policy) {
            SAME_DAY, SAME_DAY_AFTER_START, AFTER_START -> null
            DURING_EVENT -> occurrence.end.takeIf { timeProvider.nowInstant() < it }
            ANYTIME -> Instant.MAX
        }
}
