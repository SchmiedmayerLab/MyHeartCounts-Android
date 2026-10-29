//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.grovealliance.health.FakeHealth
import org.grovealliance.health.RecordType
import org.junit.Test
import java.time.Instant

class DemographicsAutofillTest {
    private val health = FakeHealth()
    private val access = FakeDemographicsHealthAccess()
    private val autofill = DemographicsAutofillImpl(
        health = health,
        healthAccess = access,
    )

    @Test
    fun `requests access and reads the most recent height and weight`() = runTest {
        // given
        health.setRecords(
            type = RecordType.height,
            records = listOf(
                heightRecord(meters = 1.5),
                heightRecord(meters = 1.75),
            ),
        )
        health.setRecords(
            type = RecordType.weight,
            records = listOf(weightRecord(kilograms = 70.0)),
        )

        // when
        val result = autofill.read()

        // then
        assertThat(access.requests).isEqualTo(1)
        assertThat(result.getOrThrow()).isEqualTo(
            DemographicsMeasurements(
                heightInCm = 175.0,
                weightInKg = 70.0,
            ),
        )
    }

    @Test
    fun `a measurement without records stays missing`() = runTest {
        // given
        health.setRecords(
            type = RecordType.height,
            records = listOf(heightRecord(meters = 1.75)),
        )

        // when
        val result = autofill.read()

        // then
        assertThat(result.getOrThrow()).isEqualTo(
            DemographicsMeasurements(
                heightInCm = 175.0,
                weightInKg = null,
            ),
        )
    }

    @Test
    fun `failing reads fail the result`() = runTest {
        // given
        health.setQueryFailure(error = IllegalStateException("unavailable"))

        // when
        val result = autofill.read()

        // then
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `a failed access request fails without reading`() = runTest {
        // given
        access.setRequestFailure(error = IllegalStateException("no activity"))

        // when
        val result = autofill.read()

        // then
        assertThat(result.isFailure).isTrue()
        assertThat(health.queries).isEmpty()
    }

    private fun heightRecord(meters: Double) = HeightRecord(
        time = Instant.parse("2026-01-01T00:00:00Z"),
        zoneOffset = null,
        height = Length.meters(meters),
        metadata = Metadata.manualEntry(),
    )

    private fun weightRecord(kilograms: Double) = WeightRecord(
        time = Instant.parse("2026-01-01T00:00:00Z"),
        zoneOffset = null,
        weight = Mass.kilograms(kilograms),
        metadata = Metadata.manualEntry(),
    )
}
