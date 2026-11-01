//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import org.grovealliance.scheduler.InstantRange
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * How a quantity's stored summaries are reduced to one value.
 */
enum class HealthStatsAggregation {
    Sum,
    Min,
    Max,
    Avg,
}

/**
 * Which sources take part in a result, and what happens when several contributed to the same time.
 */
sealed interface HealthStatsSourcePolicy {

    /**
     * Combines contributions where the metric allows it; otherwise keeps the preferred source and
     * reports the fallback as a [HealthStatsDiagnostic.PreferredSourceFallback].
     */
    data object Automatic : HealthStatsSourcePolicy

    /**
     * Uses the contribution of [sourceId] alone.
     */
    data class Only(val sourceId: String) : HealthStatsSourcePolicy

    /**
     * Prefers [sourceIds] in order, then the default order, filling time a preferred source does
     * not cover from the others without combining competing values.
     */
    data class Preferred(val sourceIds: List<String>) : HealthStatsSourcePolicy

    /**
     * Requires competing contributions to combine exactly, failing with a
     * [HealthStatsProcessingException] where [Automatic] would fall back to one source.
     */
    data object MergeCompatible : HealthStatsSourcePolicy
}

/**
 * Calendar intervals a quantity's values are grouped into, aligned to [anchor] in the participant's
 * time zone.
 *
 * Grouping preserves the weights of averages, so that a mean over several stored summaries stays
 * exact where the summaries allow it.
 *
 * @property length how many [unit]s one interval spans
 * @property unit the calendar unit of an interval; [ChronoUnit.MINUTES] up to [ChronoUnit.YEARS]
 * @property anchor a boundary of the intervals, which may lie before or after the data
 * @property alignmentPolicy what happens to stored summaries straddling an interval boundary
 */
data class HealthStatsAggregationInterval(
    val length: Long = 1,
    val unit: ChronoUnit,
    val anchor: Instant,
    val alignmentPolicy: AlignmentPolicy = AlignmentPolicy.PreserveBuckets,
) {

    /**
     * What happens to stored summaries that straddle an interval boundary, such as after the
     * participant travelled between time zones.
     */
    enum class AlignmentPolicy {
        /**
         * Keeps the stored summaries ungrouped and reports [HealthStatsDiagnostic.UnalignedInterval].
         */
        PreserveBuckets,

        /**
         * Fails with [HealthStatsProcessingException.UnalignedAggregationInterval].
         */
        RequireExact,

        /**
         * Splits sums in proportion to the overlap, keeps other values whole, and reports each
         * affected interval as a [HealthStatsDiagnostic.ApproximateInterval]. Never groups into
         * intervals shorter than the stored summaries.
         */
        Approximate,
    }
}

/**
 * Where a one-time read may take its documents from.
 */
enum class HealthStatsReadPolicy {
    /**
     * Only the server; fails while offline.
     */
    Server,

    /**
     * The server, falling back to the local cache when it cannot be reached.
     */
    ServerOrCache,

    /**
     * Only the local cache.
     */
    Cache,
}

/**
 * A remark on how a result was put together: data that was left out, approximated or chosen over
 * a competing source.
 */
sealed interface HealthStatsDiagnostic {

    /**
     * [count] documents were left out because they could not be read or describe another metric or
     * version.
     */
    data class InvalidDocumentCount(val count: Int) : HealthStatsDiagnostic

    /**
     * [count] entries were left out because they could not be read or do not fit the request.
     */
    data class MalformedEntryCount(val count: Int) : HealthStatsDiagnostic

    /**
     * Competing contributions for [timeRange] could not be combined, so the preferred source's was
     * kept.
     */
    data class PreferredSourceFallback(
        val timeRange: InstantRange,
        val reason: String,
    ) : HealthStatsDiagnostic

    /**
     * The mean for [timeRange] treats its summaries as equally weighted, as their weights are not
     * known.
     */
    data class ApproximateAverage(val timeRange: InstantRange) : HealthStatsDiagnostic

    /**
     * The value for [timeRange] includes parts of summaries straddling its bounds, estimated
     * because their distribution within the summary is not known.
     */
    data class ApproximateInterval(val timeRange: InstantRange) : HealthStatsDiagnostic

    /**
     * The summaries were kept ungrouped because grouping them into the requested intervals would
     * split them.
     */
    data object UnalignedInterval : HealthStatsDiagnostic

    /**
     * The summary for [timeRange] reaches beyond the requested time, and its values cover all of it.
     */
    data class PartialBucket(val timeRange: InstantRange) : HealthStatsDiagnostic
}

/**
 * Raised when documents cannot be processed as a request demands.
 */
sealed class HealthStatsProcessingException(message: String) : Exception(message) {

    /**
     * Contributions for [timeRange] compete but could not be combined.
     */
    class IncompatibleSources(
        val timeRange: InstantRange,
        reason: String,
    ) : HealthStatsProcessingException(reason)

    /**
     * The requested intervals cannot be laid out, for example because they have no length.
     */
    class InvalidAggregationInterval : HealthStatsProcessingException("Invalid aggregation interval")

    /**
     * Grouping into the requested intervals would split stored summaries.
     */
    class UnalignedAggregationInterval : HealthStatsProcessingException("Unaligned aggregation interval")
}
