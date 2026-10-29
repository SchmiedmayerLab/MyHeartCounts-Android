//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import edu.stanford.myheartcounts.model.HealthUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import org.grovealliance.scheduler.InstantRange
import java.time.Instant

/**
 * Builds health statistics documents and entries for tests, on a clock counted in hours from
 * [epoch].
 */
object HealthStatsFixtures {

    const val HEALTH_KIT = "com.apple.HealthKit"
    const val WEARABLE = "wearable"
    const val WEIGHTING = "test.temporal.v1"

    private const val SECONDS_PER_HOUR = 3_600.0

    /**
     * The instant [hour] hours after [epoch].
     */
    fun date(hour: Double, epoch: Instant = Instant.EPOCH): Instant =
        epoch.plusMillis((hour * SECONDS_PER_HOUR * 1_000).toLong())

    /**
     * The range from [startHour] up to [endHour].
     */
    fun range(startHour: Double, endHour: Double, epoch: Instant = Instant.EPOCH) = InstantRange(
        start = date(hour = startHour, epoch = epoch),
        endExclusive = date(hour = endHour, epoch = epoch),
    )

    fun document(metricId: String, sources: Map<String, List<HealthStatsEntry>>, version: Int = 0) =
        HealthStatsDocument(
            version = version,
            metric = metricId,
            entriesBySourceId = sources,
        )

    /**
     * An hour-long total starting at [hour].
     */
    fun sumBucket(hour: Double, amount: Double, unit: HealthUnit = HealthUnit.Count) =
        HealthStatsEntry.Aggregate(
            start = date(hour = hour),
            end = date(hour = hour + 1),
            unit = unit,
            values = HealthStatsEntry.Aggregate.Values.Sum(sum = amount),
        )

    /**
     * An hour-long heart rate summary starting at [hour] whose minimum, maximum and mean are all
     * [amount].
     */
    fun bucket(hour: Double, amount: Double, average: HealthStatsAverage? = null) = HealthStatsEntry.Aggregate(
        start = date(hour = hour),
        end = date(hour = hour + 1),
        unit = HealthUnit.CountPerMinute,
        values = HealthStatsEntry.Aggregate.Values.MinMaxAvg(
            min = amount,
            max = amount,
            avg = amount,
            average = average,
        ),
    )

    /**
     * A [bucket] whose mean carries [weight].
     */
    fun weightedBucket(hour: Double, amount: Double, weight: Double) = bucket(
        hour = hour,
        amount = amount,
        average = HealthStatsAverage(
            numerator = amount * weight,
            denominator = weight,
            weighting = WEIGHTING,
        ),
    )

    /**
     * A weight reading in kilograms at [hour].
     */
    fun observation(hour: Double, amount: Double) = HealthStatsEntry.Quantity(
        date = date(hour = hour),
        unit = HealthUnit.Kilogram,
        value = amount,
    )

    /**
     * The field values of the JSON object [json], typed as a stored document's are: whole numbers
     * as [Long], other numbers as [Double].
     */
    @Suppress("UNCHECKED_CAST")
    fun fields(json: String): Map<String, Any?> = value(Json.parseToJsonElement(json)) as Map<String, Any?>

    private fun value(element: JsonElement): Any? = when (element) {
        is JsonNull -> null
        is JsonObject -> element.mapValues { value(it.value) }
        is JsonArray -> element.map(::value)
        is JsonPrimitive -> when {
            element.isString -> element.content
            element.booleanOrNull != null -> element.booleanOrNull
            element.longOrNull != null -> element.longOrNull
            else -> element.doubleOrNull
        }
    }
}
