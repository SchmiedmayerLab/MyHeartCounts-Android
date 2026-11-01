//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.healthstats.HealthStatsFixtures.HEALTH_KIT
import edu.stanford.myheartcounts.healthstats.HealthStatsFixtures.fields
import edu.stanford.myheartcounts.model.HealthUnit
import org.junit.Test
import java.time.Instant

class HealthStatsDocumentDecoderTest {

    private val entryDecoder = HealthStatsEntryDecoderImpl()
    private val decoder = HealthStatsDocumentDecoderImpl(entryDecoder = entryDecoder)

    @Test
    fun `reads an hourly total`() {
        // when
        val document = decoder.decode(
            data = monthlyDocument(
                entry = """{"start":"1970-01-01T00:00:00Z","end":"1970-01-01T01:00:00Z","unit":"count","sum":123}""",
                kind = "hourly",
                metric = "steps",
            ),
        ).getOrThrow()

        // then
        assertThat(document.malformedEntryCount).isEqualTo(0)
        assertThat(document.entriesBySourceId[HEALTH_KIT]).containsExactly(
            HealthStatsEntry.Aggregate(
                start = Instant.EPOCH,
                end = Instant.ofEpochSecond(3_600),
                unit = HealthUnit.Count,
                values = HealthStatsEntry.Aggregate.Values.Sum(sum = 123.0),
            )
        )
    }

    @Test
    fun `reads an hourly summary without weights`() {
        // when
        val entry = entryDecoder.decode(
            raw = fields(
                json = """{"start":"1970-01-01T00:00:00Z","end":"1970-01-01T01:00:00Z","unit":"count/min",""" +
                    """"min":60,"max":100,"avg":75}""",
            ),
        )

        // then
        assertThat((entry as HealthStatsEntry.Aggregate).values).isEqualTo(
            HealthStatsEntry.Aggregate.Values.MinMaxAvg(
                min = 60.0,
                max = 100.0,
                avg = 75.0,
            )
        )
        assertThat(entry.unit).isEqualTo(HealthUnit.CountPerMinute)
    }

    @Test
    fun `keeps the weights of an hourly summary`() {
        // when
        val entry = entryDecoder.decode(
            raw = fields(
                json = """{"start":"1970-01-01T00:00:00Z","end":"1970-01-01T01:00:00Z","unit":"count/min",""" +
                    """"min":60,"max":100,"avg":75,""" +
                    """"average":{"numerator":2250,"denominator":30,"weighting":"test.observation-mean.v1"}}""",
            ),
        )

        // then
        assertThat((entry as HealthStatsEntry.Aggregate).values).isEqualTo(
            HealthStatsEntry.Aggregate.Values.MinMaxAvg(
                min = 60.0,
                max = 100.0,
                avg = 75.0,
                average = HealthStatsAverage(
                    numerator = 2_250.0,
                    denominator = 30.0,
                    weighting = "test.observation-mean.v1",
                ),
            )
        )
    }

    @Test
    fun `reads an individual reading`() {
        // when
        val entry = entryDecoder.decode(
            raw = fields(json = """{"date":"1970-01-01T00:00:00Z","unit":"kg","value":72.5}"""),
        )

        // then
        assertThat(entry).isEqualTo(
            HealthStatsEntry.Quantity(
                date = Instant.EPOCH,
                unit = HealthUnit.Kilogram,
                value = 72.5,
            )
        )
    }

    @Test
    fun `reads a blood pressure reading`() {
        // when
        val entry = entryDecoder.decode(
            raw = fields(json = """{"date":"1970-01-01T00:00:00Z","unit":"mmHg","systolic":120,"diastolic":80}"""),
        )

        // then
        assertThat(entry).isEqualTo(
            HealthStatsEntry.BloodPressure(
                date = Instant.EPOCH,
                unit = HealthUnit.MillimeterOfMercury,
                systolic = 120.0,
                diastolic = 80.0,
            )
        )
    }

    @Test
    fun `keeps fractional seconds and ignores unknown fields`() {
        // given
        val expectedDate = Instant.ofEpochMilli(125)

        // when
        val aggregate = entryDecoder.decode(
            raw = fields(
                json = """{"start":"1970-01-01T01:00:00.125+01:00","end":"1970-01-01T01:00:00.625+01:00",""" +
                    """"unit":"count","sum":100,"futureMetadata":{"version":1}}""",
            ),
        )
        val quantity = entryDecoder.decode(
            raw = fields(
                json = """{"date":"1970-01-01T00:00:00.125Z","unit":"kg","value":70,"futureMetadata":{"version":1}}""",
            ),
        )
        val bloodPressure = entryDecoder.decode(
            raw = fields(
                json = """{"date":"1970-01-01T00:00:00.125Z","unit":"mmHg","systolic":120,"diastolic":80,""" +
                    """"futureMetadata":{"version":1}}""",
            ),
        )

        // then
        assertThat((aggregate as HealthStatsEntry.Aggregate).start).isEqualTo(expectedDate)
        assertThat(aggregate.end).isEqualTo(Instant.ofEpochMilli(625))
        assertThat((quantity as HealthStatsEntry.Quantity).date).isEqualTo(expectedDate)
        assertThat(quantity.value).isEqualTo(70.0)
        assertThat((bloodPressure as HealthStatsEntry.BloodPressure).date).isEqualTo(expectedDate)
        assertThat(bloodPressure.systolic).isEqualTo(120.0)
        assertThat(bloodPressure.diastolic).isEqualTo(80.0)
    }

    @Test
    fun `leaves out malformed readings and keeps their valid siblings`() {
        // given
        val json = """
            {"version":0,"metric":"weight","samples":{"com.apple.HealthKit":[
              {"date":"1970-01-01T00:00:00Z","unit":"kg","value":70},
              {"date":"1970-01-01T00:00:00Z","unit":"kg","value":99,"systolic":120,"diastolic":80},
              {"date":"1970-01-01T00:00:00Z","unit":"kg"},
              {"date":"1970-01-01T00:00:00Z","unit":"invalid-unit","value":99},
              {"date":"1970-01-01T00:00:00Z","unit":"mmHg","systolic":120},
              {"date":"1970-01-01T01:00:00Z","unit":"kg","value":71}
            ]}}
        """.trimIndent()

        // when
        val document = decoder.decode(data = fields(json = json)).getOrThrow()

        // then
        assertThat(document.malformedEntryCount).isEqualTo(4)
        assertThat(
            document.entriesBySourceId.getValue(HEALTH_KIT).map { (it as HealthStatsEntry.Quantity).value }
        ).containsExactly(70.0, 71.0).inOrder()
    }

    @Test
    fun `leaves out malformed summaries and keeps their valid siblings`() {
        // given
        val json = """
            {"version":0,"metric":"steps","hourly":{"com.apple.HealthKit":[
              {"start":"1970-01-01T00:00:00Z","end":"1970-01-01T01:00:00Z","unit":"count","sum":100},
              {"start":"1970-01-01T00:00:00Z","end":"1970-01-01T01:00:00Z","unit":"count","sum":100,"min":60,"max":100,"avg":75},
              {"start":"1970-01-01T00:00:00Z","end":"1970-01-01T01:00:00Z","unit":"count","min":60,"avg":75},
              {"start":"1970-01-01T01:00:00Z","end":"1970-01-01T00:00:00Z","unit":"count","sum":99},
              {"start":"1970-01-01T00:00:00Z","end":"1970-01-01T00:00:00Z","unit":"count","sum":99}
            ]}}
        """.trimIndent()

        // when
        val document = decoder.decode(data = fields(json = json)).getOrThrow()

        // then
        assertThat(document.malformedEntryCount).isEqualTo(4)
        assertThat(document.entriesBySourceId[HEALTH_KIT]).containsExactly(
            HealthStatsEntry.Aggregate(
                start = Instant.EPOCH,
                end = Instant.ofEpochSecond(3_600),
                unit = HealthUnit.Count,
                values = HealthStatsEntry.Aggregate.Values.Sum(sum = 100.0),
            )
        )
    }

    @Test
    fun `leaves out entries with unreadable dates or values`() {
        // given
        val json = """
            {"version":0,"metric":"steps","hourly":{"com.apple.HealthKit":[
              {"start":"1970-01-01T00:00:00Z","end":"1970-01-01T01:00:00Z","sum":100,"unit":"count"},
              {"start":"not a date","sum":200,"unit":"count"},
              {"start":"1970-01-01T00:00:00Z","sum":"invalid","unit":"count"}
            ]}}
        """.trimIndent()

        // when
        val document = decoder.decode(data = fields(json = json)).getOrThrow()

        // then
        assertThat(document.malformedEntryCount).isEqualTo(2)
        assertThat(document.entriesBySourceId.getValue(HEALTH_KIT)).hasSize(1)
    }

    @Test
    fun `rejects a document without exactly one kind of entries`() {
        // given
        val withoutEntries = """{"version":0,"metric":"steps"}"""
        val withTwoKinds = """{"version":0,"metric":"steps","hourly":{},"daily":{}}"""

        // when
        val results = listOf(withoutEntries, withTwoKinds).map { decoder.decode(data = fields(json = it)) }

        // then
        results.forEach { assertThat(it.exceptionOrNull()).isInstanceOf(HealthStatsDecodingException::class.java) }
    }

    @Test
    fun `reads workouts and recordings, including their earlier identity format`() {
        // when
        val workout = entryDecoder.decode(
            raw = fields(
                json = """{"date":"1970-01-01T00:00:00Z","endDate":"1970-01-01T01:00:00Z","unit":"s","value":900,""" +
                    """"duration":900,"activityType":52,"provenance":{"observationID":"legacy"}}""",
            ),
        )
        val recording = entryDecoder.decode(
            raw = fields(
                json = """{"id":"ecg","date":"1970-01-01T00:00:00Z","endDate":"1970-01-01T00:00:30Z",""" +
                    """"unit":"count","value":1}""",
            ),
        )

        // then
        assertThat(workout).isEqualTo(
            HealthStatsEntry.Workout(
                id = "legacy",
                date = Instant.EPOCH,
                endDate = Instant.ofEpochSecond(3_600),
                durationSeconds = 900.0,
                activityType = 52,
            )
        )
        assertThat(recording).isEqualTo(
            HealthStatsEntry.Electrocardiogram(
                id = "ecg",
                date = Instant.EPOCH,
                endDate = Instant.ofEpochSecond(30),
            )
        )
    }

    private fun monthlyDocument(entry: String, kind: String, metric: String): Map<String, Any?> =
        fields(json = """{"version":0,"metric":"$metric","$kind":{"$HEALTH_KIT":[$entry]}}""")
}
