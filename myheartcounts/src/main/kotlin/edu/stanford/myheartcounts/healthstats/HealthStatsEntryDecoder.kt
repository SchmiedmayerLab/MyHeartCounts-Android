//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("detekt:ReturnCount")

package edu.stanford.myheartcounts.healthstats

import edu.stanford.myheartcounts.firebase.FirebaseSchema.StatsEntry.ACTIVITY_TYPE
import edu.stanford.myheartcounts.firebase.FirebaseSchema.StatsEntry.AVERAGE
import edu.stanford.myheartcounts.firebase.FirebaseSchema.StatsEntry.AVG
import edu.stanford.myheartcounts.firebase.FirebaseSchema.StatsEntry.DATE
import edu.stanford.myheartcounts.firebase.FirebaseSchema.StatsEntry.DENOMINATOR
import edu.stanford.myheartcounts.firebase.FirebaseSchema.StatsEntry.DIASTOLIC
import edu.stanford.myheartcounts.firebase.FirebaseSchema.StatsEntry.DURATION
import edu.stanford.myheartcounts.firebase.FirebaseSchema.StatsEntry.END
import edu.stanford.myheartcounts.firebase.FirebaseSchema.StatsEntry.END_DATE
import edu.stanford.myheartcounts.firebase.FirebaseSchema.StatsEntry.ID
import edu.stanford.myheartcounts.firebase.FirebaseSchema.StatsEntry.MAX
import edu.stanford.myheartcounts.firebase.FirebaseSchema.StatsEntry.MIN
import edu.stanford.myheartcounts.firebase.FirebaseSchema.StatsEntry.NUMERATOR
import edu.stanford.myheartcounts.firebase.FirebaseSchema.StatsEntry.OBSERVATION_ID
import edu.stanford.myheartcounts.firebase.FirebaseSchema.StatsEntry.PROVENANCE
import edu.stanford.myheartcounts.firebase.FirebaseSchema.StatsEntry.START
import edu.stanford.myheartcounts.firebase.FirebaseSchema.StatsEntry.SUM
import edu.stanford.myheartcounts.firebase.FirebaseSchema.StatsEntry.SYSTOLIC
import edu.stanford.myheartcounts.firebase.FirebaseSchema.StatsEntry.UNIT
import edu.stanford.myheartcounts.firebase.FirebaseSchema.StatsEntry.VALUE
import edu.stanford.myheartcounts.firebase.FirebaseSchema.StatsEntry.WEIGHTING
import edu.stanford.myheartcounts.model.HealthUnit
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import kotlin.math.abs
import kotlin.math.max

/**
 * Reads single [HealthStatsEntry]s from their stored field values.
 *
 * The kind of entry is told apart by the fields it carries; an entry that also carries a field of
 * another kind is rejected, while fields the reader does not know are ignored.
 */
interface HealthStatsEntryDecoder {

    /**
     * The entry [raw] describes, or `null` when it cannot be read.
     */
    fun decode(raw: Any?): HealthStatsEntry?
}

class HealthStatsEntryDecoderImpl : HealthStatsEntryDecoder {

    override fun decode(raw: Any?): HealthStatsEntry? {
        @Suppress("UNCHECKED_CAST")
        val entry = raw as? Map<String, Any?> ?: return null
        return when {
            START in entry || END in entry -> decodeAggregate(entry)
            SYSTOLIC in entry || DIASTOLIC in entry -> decodeBloodPressure(entry)
            DURATION in entry || ACTIVITY_TYPE in entry -> decodeWorkout(entry)
            END_DATE in entry -> decodeElectrocardiogram(entry)
            else -> decodeQuantity(entry)
        }
    }

    private fun decodeAggregate(entry: Map<String, Any?>): HealthStatsEntry.Aggregate? {
        val start = entry.date(START) ?: return null
        val end = entry.date(END) ?: return null
        if (start >= end) return null
        val unit = entry.unit() ?: return null
        val values = if (SUM in entry) {
            if (!entry.hasShape(START, END, UNIT, SUM)) return null
            HealthStatsEntry.Aggregate.Values.Sum(sum = entry.double(SUM) ?: return null)
        } else {
            if (!entry.hasShape(START, END, UNIT, MIN, MAX, AVG, AVERAGE)) return null
            HealthStatsEntry.Aggregate.Values.MinMaxAvg(
                min = entry.double(MIN) ?: return null,
                max = entry.double(MAX) ?: return null,
                avg = entry.double(AVG) ?: return null,
                average = entry[AVERAGE]?.let { decodeAverage(it) ?: return null },
            )
        }
        return HealthStatsEntry.Aggregate(
            start = start,
            end = end,
            unit = unit,
            values = values,
        )
    }

    private fun decodeAverage(raw: Any): HealthStatsAverage? {
        @Suppress("UNCHECKED_CAST")
        val average = raw as? Map<String, Any?> ?: return null
        return HealthStatsAverage(
            numerator = average.double(NUMERATOR) ?: return null,
            denominator = average.double(DENOMINATOR) ?: return null,
            weighting = average.string(WEIGHTING) ?: return null,
        )
    }

    private fun decodeQuantity(entry: Map<String, Any?>): HealthStatsEntry.Quantity? {
        if (!entry.hasShape(DATE, UNIT, VALUE)) return null
        return HealthStatsEntry.Quantity(
            date = entry.date(DATE) ?: return null,
            unit = entry.unit() ?: return null,
            value = entry.double(VALUE) ?: return null,
        )
    }

    private fun decodeBloodPressure(entry: Map<String, Any?>): HealthStatsEntry.BloodPressure? {
        if (!entry.hasShape(DATE, UNIT, SYSTOLIC, DIASTOLIC)) return null
        return HealthStatsEntry.BloodPressure(
            date = entry.date(DATE) ?: return null,
            unit = entry.unit() ?: return null,
            systolic = entry.double(SYSTOLIC) ?: return null,
            diastolic = entry.double(DIASTOLIC) ?: return null,
        )
    }

    private fun decodeWorkout(entry: Map<String, Any?>): HealthStatsEntry.Workout? {
        if (!entry.hasShape(ID, DATE, END_DATE, UNIT, VALUE, DURATION, ACTIVITY_TYPE)) return null
        val workout = HealthStatsEntry.Workout(
            id = entry.eventIdentity() ?: return null,
            date = entry.date(DATE) ?: return null,
            endDate = entry.date(END_DATE) ?: return null,
            durationSeconds = entry.double(DURATION) ?: return null,
            activityType = entry.integer(ACTIVITY_TYPE)?.takeIf { it >= 0 } ?: return null,
        )
        val unit = entry.unit() ?: return null
        val seconds = entry.double(VALUE)?.let { unit.convert(value = it, target = HealthUnit.Second) }
            ?: return null
        val duration = workout.durationSeconds
        val isValid = workout.id.isNotEmpty() && workout.endDate >= workout.date && duration.isFinite() &&
            duration >= 0 && abs(seconds - duration) <= max(1.0, duration) * TOLERANCE
        return workout.takeIf { isValid }
    }

    private fun decodeElectrocardiogram(entry: Map<String, Any?>): HealthStatsEntry.Electrocardiogram? {
        if (!entry.hasShape(ID, DATE, END_DATE, UNIT, VALUE)) return null
        val recording = HealthStatsEntry.Electrocardiogram(
            id = entry.eventIdentity() ?: return null,
            date = entry.date(DATE) ?: return null,
            endDate = entry.date(END_DATE) ?: return null,
        )
        val unit = entry.unit() ?: return null
        val count = entry.double(VALUE)?.let { unit.convert(value = it, target = HealthUnit.Count) }
            ?: return null
        val isValid = recording.id.isNotEmpty() && recording.endDate >= recording.date && count == 1.0
        return recording.takeIf { isValid }
    }

    /**
     * The event's identifier, read from the provenance of entries written before events carried one
     * of their own.
     */
    private fun Map<String, Any?>.eventIdentity(): String? {
        if (this[ID] != null) return string(ID)
        val provenance = this[PROVENANCE] as? Map<*, *> ?: return null
        return provenance[OBSERVATION_ID] as? String
    }

    /**
     * Whether the entry carries no field of another entry kind.
     */
    private fun Map<String, Any?>.hasShape(vararg allowed: String): Boolean =
        keys.none { it in KNOWN_ENTRY_KEYS && it !in allowed }

    private fun Map<String, Any?>.date(key: String): Instant? {
        val text = string(key) ?: return null
        return try {
            OffsetDateTime.parse(text).toInstant()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun Map<String, Any?>.unit(): HealthUnit? = string(UNIT)?.let(HealthUnit::fromSymbol)

    private companion object {
        const val TOLERANCE = 1e-9

        val KNOWN_ENTRY_KEYS = setOf(
            START, END, UNIT, SUM, MIN, MAX, AVG, AVERAGE, DATE, VALUE, SYSTOLIC, DIASTOLIC, ID, END_DATE,
            DURATION, ACTIVITY_TYPE,
        )
    }
}

/**
 * The text stored under [key], or `null` when it holds no text.
 */
private fun Map<String, Any?>.string(key: String): String? = this[key] as? String

/**
 * The number stored under [key], or `null` when it holds no number.
 */
private fun Map<String, Any?>.double(key: String): Double? = (this[key] as? Number)?.toDouble()

/**
 * The whole number stored under [key], or `null` when it holds none.
 */
private fun Map<String, Any?>.integer(key: String): Long? = when (val value = this[key]) {
    is Long -> value
    is Int -> value.toLong()
    is Double -> value.takeIf { it.isFinite() && it == Math.rint(it) }?.toLong()
    else -> null
}
