//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.customsamples

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.healthstats.HealthStatsFixtures.fields
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.scheduler.InstantRange
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

class CustomSampleDecoderTest {

    private val timeProvider = FakeTimeProvider().apply { setZone(ZoneId.of("Europe/Zurich")) }
    private val decoder = CustomSampleDecoderImpl(timeProvider = timeProvider)
    private val lastTwoMonths = InstantRange(
        start = Instant.parse("2025-11-04T00:00:00Z"),
        endExclusive = Instant.parse("2026-01-04T00:00:00Z"),
    )

    @Test
    fun `reads a survey score observation`() {
        // when
        val sample = decoder.decode(
            data = scoreObservation(),
            type = CustomQuantitySampleType.DietScore,
            timeRange = lastTwoMonths,
        )

        // then
        assertThat(sample).isEqualTo(
            CustomQuantitySample(
                id = UUID.fromString(OBSERVATION_ID),
                type = CustomQuantitySampleType.DietScore,
                value = 9.0,
                timeRange = InstantRange(
                    start = Instant.parse("2026-01-02T08:30:00Z"),
                    endExclusive = Instant.parse("2026-01-02T08:30:00Z"),
                ),
            )
        )
    }

    @Test
    fun `reads an LDL observation in milligrams per deciliter`() {
        // given
        val data = fields(
            json = """
                {"resourceType":"Observation","id":"$OBSERVATION_ID","status":"final",
                 "code":{"coding":[{"code":"18262-6","system":"http://loinc.org"},
                   {"code":"MHCCustomSampleTypeBloodLipidMeasurement","system":"https://spezi.stanford.edu"}]},
                 "valueQuantity":{"value":128,"unit":"mg/dL","code":"18262-6","system":"http://loinc.org"},
                 "effectiveDateTime":"2026-01-01T12:00:00.000Z"}
            """.trimIndent(),
        )

        // when
        val sample = decoder.decode(
            data = data,
            type = CustomQuantitySampleType.BloodLipids,
            timeRange = lastTwoMonths,
        )

        // then
        assertThat(sample?.value).isEqualTo(128.0)
    }

    @Test
    fun `leaves out a value in a unit that does not convert to the type's unit`() {
        // given
        val data = scoreObservation(valueQuantity = """{"value":3.3,"unit":"mmol/L"}""")

        // when
        val sample = decoder.decode(
            data = data,
            type = CustomQuantitySampleType.BloodLipids,
            timeRange = lastTwoMonths,
        )

        // then
        assertThat(sample).isNull()
    }

    @Test
    fun `keeps only readings taken within the time range`() {
        // given
        val atStart = scoreObservation(effective = """"effectiveDateTime":"2025-11-04T00:00:00Z"""")
        val atEnd = scoreObservation(effective = """"effectiveDateTime":"2026-01-04T00:00:00Z"""")
        val before = scoreObservation(effective = """"effectiveDateTime":"2025-11-03T23:59:59Z"""")

        // when
        val samples = listOf(atStart, atEnd, before).map {
            decoder.decode(data = it, type = CustomQuantitySampleType.DietScore, timeRange = lastTwoMonths)
        }

        // then
        assertThat(samples.map { it != null }).containsExactly(true, false, false).inOrder()
    }

    @Test
    fun `keeps periods overlapping the time range`() {
        // given
        val overlapping = scoreObservation(
            effective = """"effectivePeriod":{"start":"2025-10-30T00:00:00Z","end":"2025-11-05T00:00:00Z"}""",
        )
        val outside = scoreObservation(
            effective = """"effectivePeriod":{"start":"2025-10-30T00:00:00Z","end":"2025-11-04T00:00:00Z"}""",
        )
        val inverted = scoreObservation(
            effective = """"effectivePeriod":{"start":"2025-12-02T00:00:00Z","end":"2025-12-01T00:00:00Z"}""",
        )

        // when
        val samples = listOf(overlapping, outside, inverted).map {
            decoder.decode(data = it, type = CustomQuantitySampleType.DietScore, timeRange = lastTwoMonths)
        }

        // then
        assertThat(samples.first()?.timeRange).isEqualTo(
            InstantRange(
                start = Instant.parse("2025-10-30T00:00:00Z"),
                endExclusive = Instant.parse("2025-11-05T00:00:00Z"),
            )
        )
        assertThat(samples.drop(1)).containsExactly(null, null)
    }

    @Test
    fun `filters by time only when asked to`() {
        // given
        val instant = scoreObservation(effective = """"effectiveInstant":"2026-01-02T08:30:00Z"""")

        // when
        val filtered = decoder.decode(data = instant, type = CustomQuantitySampleType.DietScore, timeRange = lastTwoMonths)
        val unfiltered = decoder.decode(data = instant, type = CustomQuantitySampleType.DietScore, timeRange = null)

        // then
        assertThat(filtered).isNull()
        assertThat(unfiltered?.timeRange?.start).isEqualTo(Instant.parse("2026-01-02T08:30:00Z"))
    }

    @Test
    fun `places a date without a time at the start of the participant's day`() {
        // given
        val data = scoreObservation(effective = """"effectiveDateTime":"2026-01-02"""")

        // when
        val sample = decoder.decode(data = data, type = CustomQuantitySampleType.DietScore, timeRange = lastTwoMonths)

        // then
        assertThat(sample?.timeRange?.start).isEqualTo(Instant.parse("2026-01-01T23:00:00Z"))
    }

    @Test
    fun `leaves out observations that are not readable quantity observations`() {
        // given
        val unreadable = listOf(
            scoreObservation(id = "not-a-uuid"),
            scoreObservation(resourceType = "QuestionnaireResponse"),
            scoreObservation(code = """{"text":"Diet"}"""),
            scoreObservation(valueQuantity = """{"value":9,"unit":"furlongs"}"""),
            scoreObservation(valueQuantity = """{"unit":"count"}"""),
            scoreObservation(
                effective = """"effectiveDateTime":"2026-01-02T08:30:00Z","effectiveInstant":"2026-01-02T08:30:00Z"""",
            ),
            fields(json = """{"resourceType":"Observation","id":"$OBSERVATION_ID","status":"final"}"""),
        )

        // when
        val samples = unreadable.map {
            decoder.decode(data = it, type = CustomQuantitySampleType.DietScore, timeRange = null)
        }

        // then
        assertThat(samples.filterNotNull()).isEmpty()
    }

    private fun scoreObservation(
        id: String = OBSERVATION_ID,
        resourceType: String = "Observation",
        code: String = """{"coding":[{"code":"MHCCustomSampleTypeDietMEPAScore","system":"https://spezi.stanford.edu"}]}""",
        valueQuantity: String = """{"value":9,"unit":"count","system":"http://unitsofmeasure.org","code":"{count}"}""",
        effective: String = """"effectiveDateTime":"2026-01-02T08:30:00.000Z"""",
    ): Map<String, Any?> = fields(
        json = """
            {"resourceType":"$resourceType","id":"$id","status":"final","code":$code,
             "subject":{"reference":"user/participant"},"valueQuantity":$valueQuantity,$effective,
             "issued":"2026-01-02T08:31:00.000Z","derivedFrom":[{"reference":"QuestionnaireResponse/response"}]}
        """.trimIndent(),
    )

    private companion object {
        const val OBSERVATION_ID = "6f1c0e2a-7d4b-4c55-9a8e-2f3b1c9d0e11"
    }
}
