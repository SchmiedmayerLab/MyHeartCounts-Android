//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import edu.stanford.myheartcounts.firebase.FirebaseSchema.StatsDocument
import edu.stanford.myheartcounts.firebase.FirebaseSchema.StatsEntry
import org.grovealliance.core.time.TimeProvider
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Encodes monthly source contributions for storage.
 */
interface HealthStatsDocumentEncoder {
    fun encode(contribution: HealthStatsContribution): Result<Map<String, Any>>
}

class HealthStatsDocumentEncoderImpl(
    private val timeProvider: TimeProvider,
    private val entryDecoder: HealthStatsEntryDecoder,
) : HealthStatsDocumentEncoder {
    override fun encode(contribution: HealthStatsContribution): Result<Map<String, Any>> {
        val entries = contribution.entries.map { entry(it) }
        val document = mapOf(
            StatsDocument.VERSION to 0,
            StatsDocument.METRIC to contribution.metricId,
            contribution.collection.field to mapOf(contribution.sourceId to entries),
        )
        return if (entries.all { finite(value = it) && entryDecoder.decode(raw = it) != null }) {
            Result.success(document)
        } else {
            Result.failure(HealthStatsDecodingException(message = "Invalid statistics contribution"))
        }
    }

    private fun finite(value: Any?): Boolean = when (value) {
        is Number -> value.toDouble().isFinite()
        is Map<*, *> -> value.values.all { finite(value = it) }
        is Iterable<*> -> value.all { finite(value = it) }
        else -> true
    }

    private fun entry(entry: HealthStatsEntry): Map<String, Any> = when (entry) {
        is HealthStatsEntry.Aggregate -> mapOf(
            StatsEntry.START to date(entry.start),
            StatsEntry.END to date(entry.end),
            StatsEntry.UNIT to entry.unit.symbols.first(),
        ) + values(entry.values)
        is HealthStatsEntry.Quantity -> mapOf(
            StatsEntry.DATE to date(entry.date),
            StatsEntry.UNIT to entry.unit.symbols.first(),
            StatsEntry.VALUE to entry.value,
        )
        is HealthStatsEntry.BloodPressure -> mapOf(
            StatsEntry.DATE to date(entry.date),
            StatsEntry.UNIT to entry.unit.symbols.first(),
            StatsEntry.SYSTOLIC to entry.systolic,
            StatsEntry.DIASTOLIC to entry.diastolic,
        )
        is HealthStatsEntry.Workout -> mapOf(
            StatsEntry.ID to entry.id,
            StatsEntry.DATE to date(entry.date),
            StatsEntry.END_DATE to date(entry.endDate),
            StatsEntry.UNIT to "s",
            StatsEntry.VALUE to entry.durationSeconds,
            StatsEntry.DURATION to entry.durationSeconds,
            StatsEntry.ACTIVITY_TYPE to entry.activityType,
        )
        is HealthStatsEntry.Electrocardiogram -> mapOf(
            StatsEntry.ID to entry.id,
            StatsEntry.DATE to date(entry.date),
            StatsEntry.END_DATE to date(entry.endDate),
            StatsEntry.UNIT to "count",
            StatsEntry.VALUE to 1.0,
        )
    }

    private fun values(values: HealthStatsEntry.Aggregate.Values): Map<String, Any> = when (values) {
        is HealthStatsEntry.Aggregate.Values.Sum -> mapOf(StatsEntry.SUM to values.sum)
        is HealthStatsEntry.Aggregate.Values.MinMaxAvg -> buildMap {
            put(StatsEntry.MIN, values.min)
            put(StatsEntry.MAX, values.max)
            put(StatsEntry.AVG, values.avg)
            values.average?.let { average ->
                put(StatsEntry.AVERAGE, mapOf(
                    StatsEntry.NUMERATOR to average.numerator,
                    StatsEntry.DENOMINATOR to average.denominator,
                    StatsEntry.WEIGHTING to average.weighting,
                ))
            }
        }
    }

    private fun date(instant: Instant): String = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
        instant.atZone(timeProvider.currentZone()),
    )
}
