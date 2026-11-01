//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.firebase.FirebaseSchema
import edu.stanford.myheartcounts.healthstats.HealthStatsFixtures.HEALTH_KIT
import edu.stanford.myheartcounts.healthstats.HealthStatsFixtures.WEARABLE
import edu.stanford.myheartcounts.healthstats.HealthStatsFixtures.WEIGHTING
import edu.stanford.myheartcounts.healthstats.HealthStatsFixtures.bucket
import edu.stanford.myheartcounts.healthstats.HealthStatsFixtures.date
import edu.stanford.myheartcounts.healthstats.HealthStatsFixtures.document
import edu.stanford.myheartcounts.healthstats.HealthStatsFixtures.observation
import edu.stanford.myheartcounts.healthstats.HealthStatsFixtures.range
import edu.stanford.myheartcounts.healthstats.HealthStatsFixtures.sumBucket
import edu.stanford.myheartcounts.healthstats.HealthStatsFixtures.weightedBucket
import edu.stanford.myheartcounts.model.HealthUnit
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.scheduler.InstantRange
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class HealthStatsProcessorTest {

    private val day = range(startHour = 0.0, endHour = 24.0)
    private val processor = HealthStatsProcessorImpl(
        sourceSelector = HealthStatsSourceSelectorImpl(),
        intervalReducer = HealthStatsIntervalReducerImpl(
            timeProvider = FakeTimeProvider().apply { setZone(ZoneOffset.UTC) },
        ),
    )

    @Test
    fun `fills gaps from other sources without adding overlapping totals`() {
        // given
        val documents = listOf(
            document(metricId = STEPS, sources = mapOf(HEALTH_KIT to listOf(sumBucket(hour = 0.0, amount = 100.0)))),
            document(
                metricId = STEPS,
                sources = mapOf(
                    WEARABLE to listOf(sumBucket(hour = 0.0, amount = 900.0), sumBucket(hour = 1.0, amount = 200.0)),
                ),
            ),
        )

        // when
        val output = processor.process(request = steps(), documents = documents).getOrThrow()

        // then
        assertThat(output.elements.map { it.value }).containsExactly(100.0, 200.0).inOrder()
        assertThat(output.contributingSourceIds).containsExactly(HEALTH_KIT, WEARABLE)
        assertThat(output.diagnostics).hasSize(1)
    }

    @Test
    fun `selects and fills deterministically under explicit policies`() {
        // given
        val documents = listOf(
            document(
                metricId = STEPS,
                sources = mapOf(
                    HEALTH_KIT to listOf(sumBucket(hour = 0.0, amount = 100.0)),
                    WEARABLE to listOf(sumBucket(hour = 1.0, amount = 200.0)),
                ),
            ),
        )
        val ties = listOf(
            document(
                metricId = STEPS,
                sources = mapOf(
                    "z" to listOf(sumBucket(hour = 0.0, amount = 300.0)),
                    "a" to listOf(sumBucket(hour = 0.0, amount = 50.0)),
                ),
            ),
        )

        // when
        val only = processor.process(
            request = steps(sourcePolicy = HealthStatsSourcePolicy.Only(sourceId = WEARABLE)),
            documents = documents,
        ).getOrThrow()
        val preferred = processor.process(
            request = steps(sourcePolicy = HealthStatsSourcePolicy.Preferred(sourceIds = listOf(WEARABLE))),
            documents = documents,
        ).getOrThrow()
        val deterministic = processor.process(request = steps(), documents = ties).getOrThrow()

        // then
        assertThat(only.elements.map { it.value }).containsExactly(200.0)
        assertThat(preferred.elements.map { it.value }).containsExactly(100.0, 200.0).inOrder()
        assertThat(deterministic.elements.first().value).isEqualTo(50.0)
    }

    @Test
    fun `fails on competing totals when sources must merge`() {
        // given
        val documents = listOf(
            document(
                metricId = STEPS,
                sources = mapOf(
                    HEALTH_KIT to listOf(sumBucket(hour = 0.0, amount = 100.0)),
                    WEARABLE to listOf(sumBucket(hour = 0.0, amount = 200.0)),
                ),
            ),
        )

        // when
        val result = processor.process(
            request = steps(sourcePolicy = HealthStatsSourcePolicy.MergeCompatible),
            documents = documents,
        )

        // then
        assertThat(result.exceptionOrNull()).isInstanceOf(HealthStatsProcessingException.IncompatibleSources::class.java)
    }

    @Test
    fun `merges heart rate extremes of aligned summaries`() {
        // given
        val documents = listOf(
            document(
                metricId = HEART_RATE,
                sources = mapOf(
                    HEALTH_KIT to listOf(bucket(hour = 0.0, amount = 70.0)),
                    WEARABLE to listOf(bucket(hour = 0.0, amount = 90.0)),
                ),
            ),
        )

        // when
        val minimum = processor.process(request = heartRate(aggregation = HealthStatsAggregation.Min), documents = documents)
            .getOrThrow()
        val maximum = processor.process(request = heartRate(aggregation = HealthStatsAggregation.Max), documents = documents)
            .getOrThrow()

        // then
        assertThat(minimum.elements.first().value).isEqualTo(70.0)
        assertThat(maximum.elements.first().value).isEqualTo(90.0)
        assertThat(maximum.contributingSourceIds).containsExactly(HEALTH_KIT, WEARABLE)
    }

    @Test
    fun `requires the same averaging method and weights to merge averages`() {
        // given
        val first = weightedBucket(hour = 0.0, amount = 60.0, weight = 1.0)
        val differentMethod = bucket(
            hour = 0.0,
            amount = 90.0,
            average = HealthStatsAverage(
                numerator = 270.0,
                denominator = 3.0,
                weighting = "other",
            ),
        )
        val unweighted = bucket(hour = 0.0, amount = 90.0)

        // when
        val results = listOf(differentMethod, unweighted).map { second ->
            processor.process(
                request = heartRate(aggregation = HealthStatsAggregation.Avg, sourcePolicy = HealthStatsSourcePolicy.MergeCompatible),
                documents = listOf(
                    document(metricId = HEART_RATE, sources = mapOf(HEALTH_KIT to listOf(first), WEARABLE to listOf(second))),
                ),
            )
        }

        // then
        results.forEach { assertThat(it.isFailure).isTrue() }
    }

    @Test
    fun `does not merge extremes of partially overlapping summaries or beyond the requested time`() {
        // given
        val shifted = listOf(
            document(
                metricId = HEART_RATE,
                sources = mapOf(
                    HEALTH_KIT to listOf(bucket(hour = 0.0, amount = 60.0)),
                    WEARABLE to listOf(bucket(hour = 0.5, amount = 90.0)),
                ),
            ),
        )
        val aligned = listOf(
            document(
                metricId = HEART_RATE,
                sources = mapOf(
                    HEALTH_KIT to listOf(bucket(hour = 0.0, amount = 60.0)),
                    WEARABLE to listOf(bucket(hour = 0.0, amount = 90.0)),
                ),
            ),
        )

        // when
        val overlapping = processor.process(request = heartRate(aggregation = HealthStatsAggregation.Max), documents = shifted)
            .getOrThrow()
        val partial = processor.process(
            request = heartRate(aggregation = HealthStatsAggregation.Max, timeRange = range(startHour = 0.5, endHour = 1.0)),
            documents = aligned,
        ).getOrThrow()

        // then
        assertThat(overlapping.elements).hasSize(1)
        assertThat(overlapping.contributingSourceIds).containsExactly(HEALTH_KIT)
        assertThat(partial.contributingSourceIds).containsExactly(HEALTH_KIT)
        assertThat(partial.diagnostics).contains(HealthStatsDiagnostic.PartialBucket(timeRange = range(startHour = 0.0, endHour = 1.0)))
    }

    @Test
    fun `keeps readings taken at different instants`() {
        // given
        val documents = listOf(
            document(
                metricId = WEIGHT,
                sources = mapOf(
                    HEALTH_KIT to listOf(observation(hour = 0.0, amount = 70.0)),
                    WEARABLE to listOf(observation(hour = 2.0, amount = 70.0), observation(hour = 1.0, amount = 80.0)),
                ),
            ),
        )

        // when
        val output = processor.process(
            request = weight(sourcePolicy = HealthStatsSourcePolicy.MergeCompatible),
            documents = documents,
        ).getOrThrow()

        // then
        assertThat(output.elements.map { it.value }).containsExactly(70.0, 80.0, 70.0).inOrder()
        assertThat(output.elements.map { it.timeRange.start })
            .containsExactly(date(hour = 0.0), date(hour = 1.0), date(hour = 2.0)).inOrder()
        assertThat(output.diagnostics).isEmpty()
    }

    @Test
    fun `prefers one of overlapping sleep sessions and fills uncovered ones`() {
        // given
        val documents = listOf(
            document(
                metricId = FirebaseSchema.StatsMetric.SLEEP,
                sources = mapOf(
                    HEALTH_KIT to listOf(sumBucket(hour = 0.0, amount = 0.5, unit = HealthUnit.Hour)),
                    WEARABLE to listOf(
                        sumBucket(hour = 0.0, amount = 0.75, unit = HealthUnit.Hour),
                        sumBucket(hour = 2.0, amount = 1.0, unit = HealthUnit.Hour),
                    ),
                ),
            ),
        )

        // when
        val output = processor.process(request = HealthStatsRequest.SleepSessions(timeRange = day), documents = documents)
            .getOrThrow()

        // then
        assertThat(output.elements.map { it.hoursAsleep }).containsExactly(0.5, 1.0).inOrder()
        assertThat(output.contributingSourceIds).containsExactly(HEALTH_KIT, WEARABLE)
    }

    @Test
    fun `leaves out blood pressure readings in an incompatible unit`() {
        // given
        val valid = HealthStatsEntry.BloodPressure(
            date = date(hour = 0.0),
            unit = HealthUnit.MillimeterOfMercury,
            systolic = 120.0,
            diastolic = 80.0,
        )
        val invalid = valid.copy(
            date = date(hour = 1.0),
            unit = HealthUnit.Kilogram,
        )

        // when
        val output = processor.process(
            request = HealthStatsRequest.BloodPressure(timeRange = day),
            documents = listOf(
                document(
                    metricId = FirebaseSchema.StatsMetric.BLOOD_PRESSURE,
                    sources = mapOf(HEALTH_KIT to listOf(valid, invalid)),
                ),
            ),
        ).getOrThrow()

        // then
        assertThat(output.elements).containsExactly(
            BloodPressureStatsSample(
                date = date(hour = 0.0),
                systolic = 120.0,
                diastolic = 80.0,
            )
        )
        assertThat(output.diagnostics).containsExactly(HealthStatsDiagnostic.MalformedEntryCount(count = 1))
    }

    @Test
    fun `reports malformed entries counted while decoding`() {
        // given
        val document = document(
            metricId = STEPS,
            sources = mapOf(HEALTH_KIT to listOf(sumBucket(hour = 0.0, amount = 100.0))),
        ).copy(malformedEntryCount = 2)

        // when
        val output = processor.process(request = steps(), documents = listOf(document)).getOrThrow()

        // then
        assertThat(output.elements).hasSize(1)
        assertThat(output.diagnostics).containsExactly(HealthStatsDiagnostic.MalformedEntryCount(count = 2))
    }

    @Test
    fun `reports documents of another metric or an unsupported version`() {
        // given
        val documents = listOf(
            document(metricId = STEPS, sources = mapOf(HEALTH_KIT to listOf(sumBucket(hour = 0.0, amount = 100.0))), version = 9),
            document(metricId = HEART_RATE, sources = emptyMap()),
        )

        // when
        val output = processor.process(request = steps(), documents = documents).getOrThrow()

        // then
        assertThat(output.elements).isEmpty()
        assertThat(output.diagnostics).containsExactly(HealthStatsDiagnostic.InvalidDocumentCount(count = 2))
    }

    @Test
    fun `lets simultaneous readings from different sources compete`() {
        // given
        val documents = listOf(
            document(
                metricId = WEIGHT,
                sources = mapOf(
                    HEALTH_KIT to listOf(observation(hour = 0.0, amount = 70.0)),
                    WEARABLE to listOf(observation(hour = 0.0, amount = 70.0), observation(hour = 1.0, amount = 80.0)),
                ),
            ),
        )

        // when
        val automatic = processor.process(request = weight(), documents = documents).getOrThrow()
        val merged = processor.process(request = weight(sourcePolicy = HealthStatsSourcePolicy.MergeCompatible), documents = documents)

        // then
        assertThat(automatic.elements.map { it.value }).containsExactly(70.0, 80.0).inOrder()
        assertThat(automatic.diagnostics).hasSize(1)
        assertThat(merged.isFailure).isTrue()
    }

    @Test
    fun `selects the preferred reading at each instant and keeps the others`() {
        // given
        val documents = listOf(
            document(
                metricId = WEIGHT,
                sources = mapOf(
                    HEALTH_KIT to listOf(observation(hour = 0.0, amount = 70.0)),
                    WEARABLE to listOf(observation(hour = 0.0, amount = 80.0), observation(hour = 1.0, amount = 90.0)),
                ),
            ),
        )

        // when
        val output = processor.process(
            request = weight(sourcePolicy = HealthStatsSourcePolicy.Preferred(sourceIds = listOf(WEARABLE))),
            documents = documents,
        ).getOrThrow()

        // then
        assertThat(output.elements.map { it.value }).containsExactly(80.0, 90.0).inOrder()
        assertThat(output.contributingSourceIds).containsExactly(WEARABLE)
        assertThat(output.diagnostics).hasSize(1)
    }

    @Test
    fun `keeps simultaneous readings of the same source under every policy`() {
        // given
        val documents = listOf(
            document(
                metricId = WEIGHT,
                sources = mapOf(
                    HEALTH_KIT to listOf(
                        observation(hour = 0.0, amount = 70.0),
                        observation(hour = 0.0, amount = 71.0),
                        observation(hour = 0.0, amount = 70.0),
                    ),
                ),
            ),
        )
        val policies = listOf(
            HealthStatsSourcePolicy.Automatic,
            HealthStatsSourcePolicy.Only(sourceId = HEALTH_KIT),
            HealthStatsSourcePolicy.Preferred(sourceIds = listOf(HEALTH_KIT)),
            HealthStatsSourcePolicy.MergeCompatible,
        )

        // when
        val outputs = policies.map { processor.process(request = weight(sourcePolicy = it), documents = documents).getOrThrow() }

        // then
        outputs.forEach { output ->
            assertThat(output.elements.map { it.value }).containsExactly(70.0, 70.0, 71.0).inOrder()
            assertThat(output.diagnostics).isEmpty()
        }
    }

    @Test
    fun `requires aligned whole summaries to merge averages`() {
        // given
        val first = weightedBucket(hour = 0.0, amount = 60.0, weight = 1.0)
        val cases = listOf(
            weightedBucket(hour = 0.5, amount = 90.0, weight = 3.0) to day,
            weightedBucket(hour = 0.0, amount = 90.0, weight = 3.0) to range(startHour = 0.5, endHour = 1.0),
        )

        // when
        val results = cases.map { (second, timeRange) ->
            processor.process(
                request = heartRate(
                    aggregation = HealthStatsAggregation.Avg,
                    sourcePolicy = HealthStatsSourcePolicy.MergeCompatible,
                    timeRange = timeRange,
                ),
                documents = listOf(
                    document(metricId = HEART_RATE, sources = mapOf(HEALTH_KIT to listOf(first), WEARABLE to listOf(second))),
                ),
            )
        }

        // then
        results.forEach { assertThat(it.isFailure).isTrue() }
    }

    @Test
    fun `does not merge averages with invalid weights`() {
        // given
        val first = weightedBucket(hour = 0.0, amount = 60.0, weight = 1.0)
        val invalidAverages = listOf(
            HealthStatsAverage(numerator = 270.0, denominator = 0.0, weighting = WEIGHTING),
            HealthStatsAverage(numerator = 200.0, denominator = 3.0, weighting = WEIGHTING),
        )

        invalidAverages.forEach { average ->
            val documents = listOf(
                document(
                    metricId = HEART_RATE,
                    sources = mapOf(
                        HEALTH_KIT to listOf(first),
                        WEARABLE to listOf(bucket(hour = 0.0, amount = 90.0, average = average)),
                    ),
                ),
            )

            // when
            val fallback = processor.process(request = heartRate(aggregation = HealthStatsAggregation.Avg), documents = documents)
                .getOrThrow()
            val merged = processor.process(
                request = heartRate(aggregation = HealthStatsAggregation.Avg, sourcePolicy = HealthStatsSourcePolicy.MergeCompatible),
                documents = documents,
            )

            // then
            assertThat(fallback.elements.first().value).isEqualTo(60.0)
            assertThat(fallback.diagnostics).hasSize(1)
            assertThat(merged.isFailure).isTrue()
        }
    }

    @Test
    fun `returns nothing for empty ranges or readings at the exclusive end`() {
        // given
        val readings = listOf(document(metricId = WEIGHT, sources = mapOf(HEALTH_KIT to listOf(observation(hour = 1.0, amount = 80.0)))))
        val totals = listOf(document(metricId = STEPS, sources = mapOf(HEALTH_KIT to listOf(sumBucket(hour = 0.0, amount = 100.0)))))

        // when
        val beforeReading = processor.process(request = weight(timeRange = range(startHour = 0.0, endHour = 1.0)), documents = readings)
        val emptyAtReading = processor.process(request = weight(timeRange = range(startHour = 1.0, endHour = 1.0)), documents = readings)
        val emptyInTotal = processor.process(request = steps(timeRange = range(startHour = 0.5, endHour = 0.5)), documents = totals)

        // then
        assertThat(beforeReading.getOrThrow().elements).isEmpty()
        assertThat(emptyAtReading.getOrThrow().elements).isEmpty()
        assertThat(emptyInTotal.getOrThrow().elements).isEmpty()
    }

    @Test
    fun `keeps the active duration of workouts across months and resolves competing sources`() {
        // given
        val shared = workout(id = "shared", hour = 1)
        val documents = listOf(
            document(metricId = FirebaseSchema.StatsMetric.WORKOUTS, sources = mapOf(HEALTH_KIT to listOf(shared))),
            document(
                metricId = FirebaseSchema.StatsMetric.WORKOUTS,
                sources = mapOf("mirror" to listOf(shared), HEALTH_KIT to listOf(workout(id = "second", hour = 3))),
            ),
        )

        // when
        val output = processor.process(request = HealthStatsRequest.Workouts(timeRange = eventDay), documents = documents)
            .getOrThrow()

        // then
        assertThat(output.elements.map { it.id }).containsExactly("shared", "second").inOrder()
        assertThat(output.elements.first().duration).isEqualTo(Duration.ofMinutes(15))
        assertThat(output.elements.first().endDate).isEqualTo(eventDate(hour = 2))
        assertThat(output.elements.first().activityType).isEqualTo(WALKING)
        assertThat(output.contributingSourceIds).containsExactly(HEALTH_KIT)
        assertThat(output.diagnostics).hasSize(1)
    }

    @Test
    fun `requires recordings to start and end in the requested time`() {
        // given
        val recordings = listOf(
            recording(id = "before", hour = -1),
            recording(id = "inside", hour = 0),
            recording(id = "after", hour = 24),
            HealthStatsEntry.Electrocardiogram(
                id = "at-end",
                date = eventDate(hour = 23),
                endDate = eventDate(hour = 24),
            ),
            HealthStatsEntry.Electrocardiogram(
                id = "incomplete",
                date = eventDate(hour = 23),
                endDate = eventDate(hour = 25),
            ),
        )

        // when
        val output = processor.process(
            request = HealthStatsRequest.Electrocardiograms(timeRange = eventDay),
            documents = listOf(
                document(
                    metricId = FirebaseSchema.StatsMetric.ELECTROCARDIOGRAMS,
                    sources = mapOf(HEALTH_KIT to recordings),
                ),
            ),
        ).getOrThrow()

        // then
        assertThat(output.elements.map { it.id }).containsExactly("inside")
        assertThat(output.elements.first().endDate).isEqualTo(eventDate(hour = 0).plusSeconds(30))
    }

    @Test
    fun `lets simultaneous recordings from different sources compete`() {
        // given
        val documents = listOf(
            document(
                metricId = FirebaseSchema.StatsMetric.ELECTROCARDIOGRAMS,
                sources = mapOf(
                    HEALTH_KIT to listOf(recording(id = "first", hour = 2)),
                    "other" to listOf(recording(id = "second", hour = 2)),
                ),
            ),
        )

        // when
        val automatic = processor.process(request = recordings(), documents = documents).getOrThrow()
        val merged = processor.process(request = recordings(sourcePolicy = HealthStatsSourcePolicy.MergeCompatible), documents = documents)
        val preferred = processor.process(
            request = recordings(sourcePolicy = HealthStatsSourcePolicy.Preferred(sourceIds = listOf("other"))),
            documents = documents,
        ).getOrThrow()
        val only = processor.process(
            request = recordings(sourcePolicy = HealthStatsSourcePolicy.Only(sourceId = "other")),
            documents = documents,
        ).getOrThrow()

        // then
        assertThat(automatic.elements.map { it.id }).containsExactly("first")
        assertThat(automatic.diagnostics).hasSize(1)
        assertThat(merged.isFailure).isTrue()
        assertThat(preferred.elements.map { it.id }).containsExactly("second")
        assertThat(only.elements.map { it.id }).containsExactly("second")
        assertThat(only.diagnostics).isEmpty()
    }

    @Test
    fun `keeps recordings of the same source and at different instants without matching identities`() {
        // given
        val documents = listOf(
            document(
                metricId = FirebaseSchema.StatsMetric.ELECTROCARDIOGRAMS,
                sources = mapOf(
                    HEALTH_KIT to listOf(recording(id = "same-id", hour = 2), recording(id = "distinct", hour = 2)),
                    "other" to listOf(recording(id = "same-id", hour = 3)),
                ),
            ),
        )

        // when
        val output = processor.process(request = recordings(), documents = documents).getOrThrow()

        // then
        assertThat(output.elements).hasSize(3)
        assertThat(output.elements.count { it.id == "same-id" }).isEqualTo(2)
        assertThat(output.elements.count { it.date == eventDate(hour = 2) }).isEqualTo(2)
        assertThat(output.contributingSourceIds).containsExactly(HEALTH_KIT, "other")
        assertThat(output.diagnostics).isEmpty()
    }

    @Test
    fun `reports malformed events and keeps the valid ones`() {
        // given
        val entries = listOf(
            workout(id = "", hour = 0),
            workout(id = "negative-duration", hour = 1).copy(durationSeconds = -1.0),
            workout(id = "infinite-duration", hour = 1).copy(durationSeconds = Double.POSITIVE_INFINITY),
            workout(id = "invalid-end", hour = 2).copy(endDate = eventDate(hour = 1)),
            HealthStatsEntry.Quantity(
                date = eventDate(hour = 3),
                unit = HealthUnit.Second,
                value = 900.0,
            ),
            HealthStatsEntry.Aggregate(
                start = eventDate(hour = 3),
                end = eventDate(hour = 4),
                unit = HealthUnit.Second,
                values = HealthStatsEntry.Aggregate.Values.Sum(sum = 900.0),
            ),
            recording(id = "wrong-event", hour = 3),
            workout(id = "valid", hour = 4),
        )

        // when
        val output = processor.process(
            request = HealthStatsRequest.Workouts(timeRange = eventDay),
            documents = listOf(document(metricId = FirebaseSchema.StatsMetric.WORKOUTS, sources = mapOf(HEALTH_KIT to entries))),
        ).getOrThrow()

        // then
        assertThat(output.elements.map { it.id }).containsExactly("valid")
        assertThat(output.diagnostics).containsExactly(HealthStatsDiagnostic.MalformedEntryCount(count = 7))
    }

    @Test
    fun `leaves event entries out of quantities even in a compatible unit`() {
        // given
        val documents = listOf(
            document(
                metricId = STEPS,
                sources = mapOf(
                    HEALTH_KIT to listOf(
                        recording(id = "wrong-shape", hour = 2),
                        HealthStatsEntry.Quantity(
                            date = eventDate(hour = 3),
                            unit = HealthUnit.Count,
                            value = 500.0,
                        ),
                    ),
                ),
            ),
        )

        // when
        val output = processor.process(request = steps(timeRange = eventDay), documents = documents).getOrThrow()

        // then
        assertThat(output.elements.map { it.value }).containsExactly(500.0)
        assertThat(output.diagnostics).containsExactly(HealthStatsDiagnostic.MalformedEntryCount(count = 1))
    }

    private val eventDay: InstantRange
        get() = range(startHour = 0.0, endHour = 24.0, epoch = EVENT_EPOCH)

    private fun eventDate(hour: Int) = date(hour = hour.toDouble(), epoch = EVENT_EPOCH)

    private fun steps(
        sourcePolicy: HealthStatsSourcePolicy = HealthStatsSourcePolicy.Automatic,
        timeRange: InstantRange = day,
    ) = HealthStatsRequest.Quantity(
        metric = HealthStatsMetric.Steps,
        timeRange = timeRange,
        aggregation = HealthStatsAggregation.Sum,
        sourcePolicy = sourcePolicy,
    )

    private fun heartRate(
        aggregation: HealthStatsAggregation,
        sourcePolicy: HealthStatsSourcePolicy = HealthStatsSourcePolicy.Automatic,
        timeRange: InstantRange = day,
    ) = HealthStatsRequest.Quantity(
        metric = HealthStatsMetric.HeartRate,
        timeRange = timeRange,
        aggregation = aggregation,
        sourcePolicy = sourcePolicy,
    )

    private fun weight(
        sourcePolicy: HealthStatsSourcePolicy = HealthStatsSourcePolicy.Automatic,
        timeRange: InstantRange = day,
    ) = HealthStatsRequest.Quantity(
        metric = HealthStatsMetric.Weight,
        timeRange = timeRange,
        aggregation = HealthStatsAggregation.Avg,
        sourcePolicy = sourcePolicy,
    )

    private fun recordings(sourcePolicy: HealthStatsSourcePolicy = HealthStatsSourcePolicy.Automatic) =
        HealthStatsRequest.Electrocardiograms(
            timeRange = eventDay,
            sourcePolicy = sourcePolicy,
        )

    private fun workout(id: String, hour: Int) = HealthStatsEntry.Workout(
        id = id,
        date = eventDate(hour = hour),
        endDate = eventDate(hour = hour + 1),
        durationSeconds = 900.0,
        activityType = WALKING,
    )

    private fun recording(id: String, hour: Int) = HealthStatsEntry.Electrocardiogram(
        id = id,
        date = eventDate(hour = hour),
        endDate = eventDate(hour = hour).plusSeconds(30),
    )

    private companion object {
        const val STEPS = "steps"
        const val HEART_RATE = "heart-rate"
        const val WEIGHT = "weight"
        const val WALKING = 52L
        val EVENT_EPOCH: Instant = Instant.ofEpochSecond(1_788_761_600)
    }
}
