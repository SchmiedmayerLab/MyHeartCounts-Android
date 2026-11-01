//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import edu.stanford.myheartcounts.model.HealthUnit
import java.time.Duration

/**
 * A processed result, before the metadata of the documents it came from is attached.
 *
 * @property elements the values, in chronological order
 * @property diagnostics remarks on left-out, approximated or source-selected data
 * @property contributingSourceIds the sources whose data the elements hold
 */
data class HealthStatsOutput<T>(
    val elements: List<T>,
    val diagnostics: List<HealthStatsDiagnostic>,
    val contributingSourceIds: Set<String>,
)

/**
 * Turns months of statistics into the values a request asks for.
 *
 * Entries are validated and competing sources resolved by a [HealthStatsSourceSelector];
 * quantities are then optionally grouped into calendar intervals.
 */
interface HealthStatsProcessor {

    /**
     * The result [request] yields from [documents], or a [HealthStatsProcessingException] when the
     * documents cannot be processed as the request demands.
     */
    fun <T> process(request: HealthStatsRequest<T>, documents: List<HealthStatsDocument>): Result<HealthStatsOutput<T>>
}

class HealthStatsProcessorImpl(
    private val sourceSelector: HealthStatsSourceSelector,
    private val intervalReducer: HealthStatsIntervalReducer,
) : HealthStatsProcessor {

    @Suppress("UNCHECKED_CAST")
    override fun <T> process(
        request: HealthStatsRequest<T>,
        documents: List<HealthStatsDocument>,
    ): Result<HealthStatsOutput<T>> {
        val output = when (request) {
            is HealthStatsRequest.Quantity -> quantity(
                request = request,
                documents = documents,
            )
            is HealthStatsRequest.SleepSessions -> process(
                documents = documents,
                input = request.input(unit = HealthUnit.Hour, aggregation = HealthStatsAggregation.Sum),
            ) { values -> values.sortedBy { it.range.endExclusive }.map(::sleepSession) }
            is HealthStatsRequest.BloodPressure -> process(
                documents = documents,
                input = request.input(unit = HealthUnit.MillimeterOfMercury, aggregation = HealthStatsAggregation.Avg),
            ) { values -> values.mapNotNull(::bloodPressure) }
            is HealthStatsRequest.Workouts -> process(
                documents = documents,
                input = request.input(unit = HealthUnit.Second, aggregation = HealthStatsAggregation.Sum),
            ) { values -> values.mapNotNull(::workout) }
            is HealthStatsRequest.Electrocardiograms -> process(
                documents = documents,
                input = request.input(unit = HealthUnit.Count, aggregation = HealthStatsAggregation.Sum),
            ) { values -> values.mapNotNull(::electrocardiogram) }
        }
        return output as Result<HealthStatsOutput<T>>
    }

    private fun quantity(
        request: HealthStatsRequest.Quantity,
        documents: List<HealthStatsDocument>,
    ): Result<HealthStatsOutput<HealthStatsQuantitySample>> {
        val input = request.input(unit = request.metric.unit, aggregation = request.aggregation)
        val selected = sourceSelector.select(
            documents = documents,
            input = input,
        ).getOrElse { return Result.failure(it) }
        val values = request.interval?.let { interval ->
            val reduced = intervalReducer.reduce(
                values = selected.values,
                input = input,
                interval = interval,
            ).getOrElse { return Result.failure(it) }
            HealthStatsValues(
                values = reduced.values,
                diagnostics = selected.diagnostics + reduced.diagnostics,
            )
        } ?: selected
        val output = output(values) { reduced ->
            reduced.map {
                HealthStatsQuantitySample(
                    timeRange = it.range,
                    value = it.amount,
                )
            }
        }
        return Result.success(output)
    }

    private inline fun <T> process(
        documents: List<HealthStatsDocument>,
        input: HealthStatsInput,
        crossinline elements: (List<HealthStatsValue>) -> List<T>,
    ): Result<HealthStatsOutput<T>> = sourceSelector.select(
        documents = documents,
        input = input,
    ).map { selected -> output(selected, elements) }

    private inline fun <T> output(
        selected: HealthStatsValues,
        elements: (List<HealthStatsValue>) -> List<T>,
    ) = HealthStatsOutput(
        elements = elements(selected.values),
        diagnostics = selected.diagnostics,
        contributingSourceIds = selected.values.flatMapTo(mutableSetOf()) { it.sources },
    )

    private fun HealthStatsRequest<*>.input(unit: HealthUnit, aggregation: HealthStatsAggregation) =
        HealthStatsInput(
            metricId = metricId,
            timeRange = timeRange,
            sourcePolicy = sourcePolicy,
            unit = unit,
            aggregation = aggregation,
        )

    private fun sleepSession(value: HealthStatsValue) = SleepSessionStatsSample(
        timeRange = value.range,
        hoursAsleep = value.amount,
    )

    private fun bloodPressure(value: HealthStatsValue): BloodPressureStatsSample? =
        value.secondaryAmount?.let { diastolic ->
            BloodPressureStatsSample(
                date = value.range.start,
                systolic = value.amount,
                diastolic = diastolic,
            )
        }

    private fun workout(value: HealthStatsValue): WorkoutStatsSample? =
        (value.event as? HealthStatsEntry.Workout)?.let { workout ->
            WorkoutStatsSample(
                id = workout.id,
                date = workout.date,
                endDate = workout.endDate,
                duration = Duration.ofNanos((workout.durationSeconds * NANOS_PER_SECOND).toLong()),
                activityType = workout.activityType,
            )
        }

    private fun electrocardiogram(value: HealthStatsValue): ElectrocardiogramStatsSample? =
        (value.event as? HealthStatsEntry.Electrocardiogram)?.let { recording ->
            ElectrocardiogramStatsSample(
                id = recording.id,
                date = recording.date,
                endDate = recording.endDate,
            )
        }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
