//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.account.UsRegionKey
import edu.stanford.myheartcounts.account.WeightInKgKey
import org.grovealliance.account.AccountKeys
import org.grovealliance.account.dateOfBirth
import org.grovealliance.core.time.FakeTimeProvider
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DemographicsCodecTest {
    private val timeProvider = FakeTimeProvider()
    private val codec = DemographicsCodecImpl(timeProvider = timeProvider)

    @Before
    fun setup() {
        timeProvider.setZone(zone = ZoneId.of("America/Los_Angeles"))
    }

    @Test
    fun `every answer round trips through the account details`() {
        // given
        val response = DemographicsResponseFixtures.answered()

        // when
        val details = codec.changes(
            baseline = DemographicsResponse.empty,
            response = response,
        ).getOrThrow().modifiedDetails

        // then
        assertThat(codec.read(details = details)).isEqualTo(response)
    }

    @Test
    fun `the date of birth is stored at local noon`() {
        // given
        val response = DemographicsResponseFixtures.create(dateOfBirth = LocalDate.of(1990, 6, 15))

        // when
        val details = codec.changes(
            baseline = DemographicsResponse.empty,
            response = response,
        ).getOrThrow().modifiedDetails

        // then
        val stored = details[AccountKeys.dateOfBirth::class]?.atZone(timeProvider.currentZone())
        assertThat(stored?.hour).isEqualTo(12)
        assertThat(stored?.toLocalDate()).isEqualTo(LocalDate.of(1990, 6, 15))
    }

    @Test
    fun `changes write changed answers and remove cleared ones only`() {
        // given
        val baseline = DemographicsResponseFixtures.answered()
        val response = baseline.copy(
            usRegion = null,
            weightInKg = 80.0,
        )

        // when
        val changes = codec.changes(
            baseline = baseline,
            response = response,
        ).getOrThrow()

        // then
        assertThat(changes.modifiedDetails.accountKeyTypes).containsExactly(WeightInKgKey::class)
        assertThat(changes.modifiedDetails[WeightInKgKey::class]).isEqualTo(80.0)
        assertThat(changes.removedAccountKeys).containsExactly(UsRegionKey::class)
    }

    @Test
    fun `differences name the questions answered differently`() {
        // given
        val first = DemographicsResponseFixtures.answered()
        val second = first.copy(
            heightInCm = 180.0,
            nhsNumber = null,
        )

        // when
        val differences = codec.differences(
            first = first,
            second = second,
        )

        // then
        assertThat(differences).containsExactly(DemographicsField.Height, DemographicsField.NHSNumber)
    }

    @Test
    fun `merging takes only the given answers from the source`() {
        // given
        val target = DemographicsResponseFixtures.answered()
        val source = DemographicsResponseFixtures.create(
            heightInCm = 180.0,
            weightInKg = 90.0,
        )

        // when
        val merged = codec.merged(
            target = target,
            source = source,
            fields = setOf(DemographicsField.Height),
        )

        // then
        assertThat(merged).isEqualTo(target.copy(heightInCm = 180.0))
    }
}
