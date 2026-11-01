//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("detekt:ReturnCount")

package edu.stanford.myheartcounts.customsamples

import edu.stanford.myheartcounts.model.HealthUnit
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.scheduler.InstantRange
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.Year
import java.time.YearMonth
import java.util.UUID

/**
 * Reads [CustomQuantitySample]s from stored FHIR `Observation`s.
 */
interface CustomSampleDecoder {

    /**
     * The sample of [type] the observation [data] describes, or `null` when it is not a readable
     * quantity observation or, given a [timeRange], does not fall into it.
     *
     * An observation taken at one instant falls into the range when the range holds that instant;
     * one spanning a period, when the period overlaps the range. An observation whose time cannot
     * be read never falls into a range.
     */
    fun decode(
        data: Map<String, Any?>,
        type: CustomQuantitySampleType,
        timeRange: InstantRange?,
    ): CustomQuantitySample?
}

class CustomSampleDecoderImpl(
    private val timeProvider: TimeProvider,
) : CustomSampleDecoder {

    override fun decode(
        data: Map<String, Any?>,
        type: CustomQuantitySampleType,
        timeRange: InstantRange?,
    ): CustomQuantitySample? {
        if (timeRange != null && !isWithin(data = data, timeRange = timeRange)) return null
        if (data[RESOURCE_TYPE] != OBSERVATION || data[STATUS] !is String) return null
        val id = (data[ID] as? String)?.let(::uuid) ?: return null
        val code = data[CODE] as? Map<*, *> ?: return null
        if (code[CODING] !is List<*>) return null
        if (data.keys.count { it.isChoiceOf(VALUE) } != 1) return null
        val quantity = data[VALUE_QUANTITY] as? Map<*, *> ?: return null
        val unit = (quantity[UNIT] as? String)?.let(HealthUnit::fromSymbol) ?: return null
        val value = (quantity[VALUE] as? Number)?.toDouble() ?: return null
        val effective = effective(data) ?: return null
        return CustomQuantitySample(
            id = id,
            type = type,
            value = unit.convert(value = value, target = type.unit) ?: return null,
            timeRange = effective,
        )
    }

    /**
     * Whether the observation's time falls into [timeRange], read before the rest of the
     * observation so that observations outside it are skipped cheaply.
     */
    private fun isWithin(data: Map<String, Any?>, timeRange: InstantRange): Boolean {
        val dateTime = data[EFFECTIVE_DATE_TIME] as? String
        val period = data[EFFECTIVE_PERIOD] as? Map<*, *>
        return when {
            dateTime != null -> parse(dateTime)?.let { it in timeRange } ?: false
            period != null -> {
                val start = (period[START] as? String)?.let(::parse) ?: return false
                val end = (period[END] as? String)?.let(::parse) ?: return false
                start < end && start < timeRange.endExclusive && timeRange.start < end
            }
            else -> false
        }
    }

    /**
     * The time the observation applies to, from its single `effective` value.
     */
    private fun effective(data: Map<String, Any?>): InstantRange? {
        if (data.keys.count { it.isChoiceOf(EFFECTIVE) } != 1) return null
        val instant = (data[EFFECTIVE_DATE_TIME] as? String ?: data[EFFECTIVE_INSTANT] as? String)?.let(::parse)
        if (instant != null) {
            return InstantRange(
                start = instant,
                endExclusive = instant,
            )
        }
        val period = data[EFFECTIVE_PERIOD] as? Map<*, *> ?: return null
        val start = (period[START] as? String)?.let(::parse) ?: return null
        val end = (period[END] as? String)?.let(::parse) ?: return null
        if (end < start) return null
        return InstantRange(
            start = start,
            endExclusive = end,
        )
    }

    /**
     * The instant a FHIR date or date-time denotes; a date without a time denotes its start in the
     * participant's time zone.
     */
    private fun parse(text: String): Instant? {
        val zone = timeProvider.currentZone()
        return try {
            when (text.length) {
                YEAR_LENGTH -> Year.parse(text).atDay(1).atStartOfDay(zone).toInstant()
                YEAR_MONTH_LENGTH -> YearMonth.parse(text).atDay(1).atStartOfDay(zone).toInstant()
                DATE_LENGTH -> LocalDate.parse(text).atStartOfDay(zone).toInstant()
                else -> OffsetDateTime.parse(text).toInstant()
            }
        } catch (_: DateTimeException) {
            null
        }
    }

    private fun uuid(text: String): UUID? = try {
        UUID.fromString(text).takeIf { it.toString().equals(text, ignoreCase = true) }
    } catch (_: IllegalArgumentException) {
        null
    }

    /**
     * Whether the key names one of the types of the FHIR choice element [element], such as
     * `valueQuantity` for `value`.
     */
    private fun String.isChoiceOf(element: String): Boolean =
        length > element.length && startsWith(element) && this[element.length].isUpperCase()

    private companion object {
        const val YEAR_LENGTH = 4
        const val YEAR_MONTH_LENGTH = 7
        const val DATE_LENGTH = 10
        const val RESOURCE_TYPE = "resourceType"
        const val OBSERVATION = "Observation"
        const val STATUS = "status"
        const val ID = "id"
        const val CODE = "code"
        const val CODING = "coding"
        const val VALUE = "value"
        const val VALUE_QUANTITY = "valueQuantity"
        const val UNIT = "unit"
        const val EFFECTIVE = "effective"
        const val EFFECTIVE_DATE_TIME = "effectiveDateTime"
        const val EFFECTIVE_INSTANT = "effectiveInstant"
        const val EFFECTIVE_PERIOD = "effectivePeriod"
        const val START = "start"
        const val END = "end"
    }
}
