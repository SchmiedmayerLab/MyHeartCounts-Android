//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import edu.stanford.myheartcounts.firebase.FirebaseSchema
import edu.stanford.myheartcounts.model.HealthUnit
import org.grovealliance.scheduler.InstantRange
import java.time.Instant
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Validates months of statistics against a request and resolves competing sources.
 *
 * Entries are converted to the requested unit, and selection works per stored summary or reading:
 * another source fills time the preferred source does not cover, while values competing for the
 * same time are never added together. Only heart rate summaries of the same interval are combined.
 */
interface HealthStatsSourceSelector {

    /**
     * The values [input] selects from [documents], in chronological order, or
     * [HealthStatsProcessingException.IncompatibleSources] when competing contributions cannot be
     * combined under [HealthStatsSourcePolicy.MergeCompatible].
     */
    fun select(documents: List<HealthStatsDocument>, input: HealthStatsInput): Result<HealthStatsValues>
}

class HealthStatsSourceSelectorImpl : HealthStatsSourceSelector {

    override fun select(documents: List<HealthStatsDocument>, input: HealthStatsInput): Result<HealthStatsValues> {
        val candidates = candidates(documents = documents, input = input)
        val diagnostics = candidates.diagnostics.toMutableList()
        val selected = Selection()
        for (value in candidates.values) {
            val conflicts = selected.conflictingIndices(value)
            val conflictIndex = conflicts.singleOrNull()
            val merged = conflictIndex?.let {
                mergedSources(
                    lhs = selected.values[it],
                    rhs = value,
                    input = input,
                )
            }
            when {
                conflicts.isEmpty() -> selected.append(value)
                conflictIndex != null && merged != null -> selected.values[conflictIndex] = merged
                input.sourcePolicy == HealthStatsSourcePolicy.MergeCompatible -> return Result.failure(
                    HealthStatsProcessingException.IncompatibleSources(
                        timeRange = value.range,
                        reason = FALLBACK_REASON,
                    )
                )
                else -> diagnostics.add(
                    HealthStatsDiagnostic.PreferredSourceFallback(
                        timeRange = value.range,
                        reason = FALLBACK_REASON,
                    )
                )
            }
        }
        for (value in selected.values) {
            val reachesBeyond = value.range.start < input.timeRange.start ||
                value.range.endExclusive > input.timeRange.endExclusive
            if (!value.range.isEmpty && reachesBeyond) {
                diagnostics.add(HealthStatsDiagnostic.PartialBucket(timeRange = value.range))
            }
        }
        return Result.success(
            HealthStatsValues(
                values = selected.values.sortedBy { it.range.start },
                diagnostics = diagnostics,
            )
        )
    }

    /**
     * Every usable value of the documents within the requested time, ordered by source preference
     * and then chronologically, with the counts of what had to be left out.
     */
    private fun candidates(documents: List<HealthStatsDocument>, input: HealthStatsInput): HealthStatsValues {
        if (input.timeRange.isEmpty) {
            return HealthStatsValues(
                values = emptyList(),
                diagnostics = emptyList(),
            )
        }
        val values = mutableListOf<HealthStatsValue>()
        var invalidDocuments = 0
        var malformedEntries = 0
        for (document in documents) {
            if (document.version != 0 || document.metric != input.metricId) {
                invalidDocuments++
            } else {
                malformedEntries += document.malformedEntryCount
                malformedEntries += collectValues(
                    document = document,
                    input = input,
                    into = values,
                )
            }
        }
        val diagnostics = buildList {
            if (invalidDocuments > 0) add(HealthStatsDiagnostic.InvalidDocumentCount(count = invalidDocuments))
            if (malformedEntries > 0) add(HealthStatsDiagnostic.MalformedEntryCount(count = malformedEntries))
        }
        return HealthStatsValues(
            values = sorted(
                values = values,
                policy = input.sourcePolicy,
            ),
            diagnostics = diagnostics,
        )
    }

    /**
     * Adds the document's values within the requested time to [into], returning how many entries
     * did not fit the request.
     */
    private fun collectValues(
        document: HealthStatsDocument,
        input: HealthStatsInput,
        into: MutableList<HealthStatsValue>,
    ): Int {
        var malformedEntries = 0
        val policy = input.sourcePolicy
        val sources = document.entriesBySourceId.filterKeys { policy !is HealthStatsSourcePolicy.Only || it == policy.sourceId }
        for ((source, entries) in sources) {
            for (entry in entries) {
                val range = entry.timeRange
                val value = value(
                    entry = entry,
                    source = source,
                    input = input,
                )
                if (range == null || value == null) {
                    malformedEntries++
                } else if (range.overlaps(input.timeRange) && value.endsBefore(input.timeRange.endExclusive)) {
                    into.add(value)
                }
            }
        }
        return malformedEntries
    }

    /**
     * Whether the value's event, if it stands for one, ends before [end].
     */
    private fun HealthStatsValue.endsBefore(end: Instant): Boolean = when (val event = event) {
        is HealthStatsEntry.Workout -> event.endDate < end
        is HealthStatsEntry.Electrocardiogram -> event.endDate < end
        else -> true
    }

    private fun sorted(values: List<HealthStatsValue>, policy: HealthStatsSourcePolicy): List<HealthStatsValue> {
        val preferred = (policy as? HealthStatsSourcePolicy.Preferred)?.sourceIds.orEmpty()
        val sources = (preferred + DEFAULT_PREFERRED_SOURCE_ID + values.flatMap { it.sources }.toSortedSet()).distinct()
        val rank = { value: HealthStatsValue ->
            sources.indexOfFirst { it in value.sources }.takeIf { it >= 0 } ?: sources.size
        }
        return values.sortedWith(
            compareBy<HealthStatsValue> { rank(it) }
                .thenBy { it.range.start }
                .thenBy { it.range.endExclusive }
                .thenBy { it.amount },
        )
    }

    /**
     * The value [entry] contributes to [input], or `null` when the entry does not fit the request.
     */
    @Suppress("detekt:CyclomaticComplexMethod", "detekt:ReturnCount")
    private fun value(entry: HealthStatsEntry, source: String, input: HealthStatsInput): HealthStatsValue? {
        val range = entry.timeRange ?: return null
        if (!entry.unit.isCompatibleWith(input.unit)) return null
        var average: HealthStatsAverage? = null
        var event: HealthStatsEntry? = null
        val amounts: Pair<Double, Double?> = when (entry) {
            is HealthStatsEntry.Aggregate -> {
                if (input.metricId in EVENT_METRIC_IDS) return null
                val (amount, entryAverage) = aggregateValues(
                    values = entry.values,
                    aggregation = input.aggregation,
                ) ?: return null
                average = entryAverage
                amount to null
            }
            is HealthStatsEntry.Quantity -> {
                if (input.metricId in EVENT_METRIC_IDS) return null
                entry.value to null
            }
            is HealthStatsEntry.BloodPressure -> {
                if (input.metricId != FirebaseSchema.StatsMetric.BLOOD_PRESSURE) return null
                entry.systolic to entry.diastolic
            }
            is HealthStatsEntry.Workout -> {
                val isValid = input.metricId == FirebaseSchema.StatsMetric.WORKOUTS && entry.id.isNotEmpty() &&
                    entry.endDate >= entry.date && entry.durationSeconds.isFinite() && entry.durationSeconds >= 0
                if (!isValid) return null
                event = entry
                entry.durationSeconds to null
            }
            is HealthStatsEntry.Electrocardiogram -> {
                val isValid = input.metricId == FirebaseSchema.StatsMetric.ELECTROCARDIOGRAMS && entry.id.isNotEmpty() &&
                    entry.endDate >= entry.date
                if (!isValid) return null
                event = entry
                1.0 to null
            }
        }
        val (primary, secondary) = amounts
        if (!primary.isFinite() || secondary?.isFinite() == false) return null
        return HealthStatsValue(
            range = range,
            amount = entry.unit.convert(value = primary, target = input.unit) ?: return null,
            secondaryAmount = secondary?.let { entry.unit.convert(value = it, target = input.unit) ?: return null },
            average = average?.let {
                convertedAverage(
                    average = it,
                    from = entry.unit,
                    to = input.unit,
                ) ?: return null
            },
            sources = setOf(source),
            event = event,
        )
    }

    /**
     * Converts the mean rather than the numerator, keeping the weights.
     */
    private fun convertedAverage(
        average: HealthStatsAverage,
        from: HealthUnit,
        to: HealthUnit,
    ): HealthStatsAverage? {
        val mean = from.convert(value = average.numerator / average.denominator, target = to) ?: return null
        return HealthStatsAverage(
            numerator = mean * average.denominator,
            denominator = average.denominator,
            weighting = average.weighting,
        )
    }

    /**
     * The value [values] provide for [aggregation], with the average's weights when they reproduce
     * the stored mean; `null` when the summary has no such value.
     */
    private fun aggregateValues(
        values: HealthStatsEntry.Aggregate.Values,
        aggregation: HealthStatsAggregation,
    ): Pair<Double, HealthStatsAverage?>? = when (values) {
        is HealthStatsEntry.Aggregate.Values.Sum ->
            if (aggregation == HealthStatsAggregation.Sum) values.sum to null else null
        is HealthStatsEntry.Aggregate.Values.MinMaxAvg -> {
            val amount = when (aggregation) {
                HealthStatsAggregation.Sum -> null
                HealthStatsAggregation.Min -> values.min
                HealthStatsAggregation.Max -> values.max
                HealthStatsAggregation.Avg -> values.avg
            }
            val validAverage = values.average?.takeIf { average ->
                average.isValid && values.avg.isFinite() &&
                    abs(average.numerator / average.denominator - values.avg) <= max(1.0, abs(values.avg)) * TOLERANCE
            }
            amount?.let { it to validAverage }
        }
    }

    /**
     * Combines two sources' summaries of the same heart rate interval, when the request allows it
     * and the summaries carry what is needed.
     */
    private fun mergedSources(
        lhs: HealthStatsValue,
        rhs: HealthStatsValue,
        input: HealthStatsInput,
    ): HealthStatsValue? {
        val mergingAllowed = input.sourcePolicy == HealthStatsSourcePolicy.Automatic ||
            input.sourcePolicy == HealthStatsSourcePolicy.MergeCompatible
        val mergeable = mergingAllowed && input.metricId == HealthStatsMetric.HeartRate.id && !lhs.range.isEmpty &&
            lhs.range == rhs.range && lhs.range.start >= input.timeRange.start &&
            lhs.range.endExclusive <= input.timeRange.endExclusive
        if (!mergeable) return null
        val merged = when (input.aggregation) {
            HealthStatsAggregation.Min -> lhs.copy(amount = min(lhs.amount, rhs.amount))
            HealthStatsAggregation.Max -> lhs.copy(amount = max(lhs.amount, rhs.amount))
            HealthStatsAggregation.Avg -> listOf(lhs, rhs).combinedAverage()?.let { average ->
                lhs.copy(
                    average = average,
                    amount = average.numerator / average.denominator,
                )
            }
            HealthStatsAggregation.Sum -> null
        }
        return merged?.copy(sources = lhs.sources + rhs.sources)
    }

    /**
     * The values selected so far. Summaries never overlap one another, which lets a summary that
     * starts after all others be accepted without a search.
     */
    private class Selection {
        val values = mutableListOf<HealthStatsValue>()
        private val bucketIndices = mutableMapOf<InstantRange, Int>()
        private var maximumBucketEnd: Instant? = null
        private var hasObservations = false

        fun append(value: HealthStatsValue) {
            if (value.range.isEmpty) {
                hasObservations = true
            } else {
                bucketIndices[value.range] = values.size
                maximumBucketEnd = maxOf(maximumBucketEnd ?: value.range.endExclusive, value.range.endExclusive)
            }
            values.add(value)
        }

        @Suppress("detekt:ReturnCount")
        fun conflictingIndices(value: HealthStatsValue): List<Int> {
            if (!hasObservations && !value.range.isEmpty) {
                val end = maximumBucketEnd
                if (end == null || value.range.start >= end) return emptyList()
                bucketIndices[value.range]?.let { return listOf(it) }
            }
            return values.indices.filter { index ->
                val existing = values[index]
                if (value.range.isEmpty && existing.range.isEmpty) {
                    existing.sources != value.sources && existing.range.start == value.range.start
                } else {
                    existing.range.overlaps(value.range)
                }
            }
        }
    }

    private companion object {
        /**
         * The source preferred over all others unless a request names its own preference.
         */
        const val DEFAULT_PREFERRED_SOURCE_ID = FirebaseSchema.StatsSource.HEALTH_KIT
        const val FALLBACK_REASON =
            "Competing readings, overlapping totals, unaligned buckets, or incompatible average weights"
        const val TOLERANCE = 1e-9
        val EVENT_METRIC_IDS = setOf(
            FirebaseSchema.StatsMetric.BLOOD_PRESSURE,
            FirebaseSchema.StatsMetric.WORKOUTS,
            FirebaseSchema.StatsMetric.ELECTROCARDIOGRAMS,
        )
    }
}
