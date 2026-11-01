//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.model.demographics.BiologicalSex
import edu.stanford.myheartcounts.model.demographics.LatinoStatusOption
import edu.stanford.myheartcounts.model.demographics.MHCGenderIdentity
import edu.stanford.myheartcounts.model.demographics.RaceEthnicity
import org.junit.Test

class DemographicsRulesTest {
    private val rules = DemographicsRules()

    @Test
    fun `the US form asks the US questions only`() {
        // when
        val fields = rules.fields(region = DemographicsRegion.US)

        // then
        assertThat(fields).containsNoneOf(
            DemographicsField.UKRegion,
            DemographicsField.UKEducation,
            DemographicsField.UKIncome,
            DemographicsField.NHSNumber,
        )
        assertThat(fields).containsAtLeast(DemographicsField.LatinoStatus, DemographicsField.USRegion)
    }

    @Test
    fun `the UK form asks the UK questions only`() {
        // when
        val fields = rules.fields(region = DemographicsRegion.UK)

        // then
        assertThat(fields).containsNoneOf(
            DemographicsField.LatinoStatus,
            DemographicsField.USRegion,
            DemographicsField.USEducation,
            DemographicsField.USIncome,
        )
        assertThat(fields).containsAtLeast(DemographicsField.UKRegion, DemographicsField.NHSNumber)
    }

    @Test
    fun `an empty US form requires the core questions and Latino status`() {
        // given
        val state = DemographicsStateFixtures.create(response = DemographicsResponse.empty)

        // when
        val completion = rules.completion(state = state)

        // then
        assertThat(completion.missingRequired()).containsExactly(
            DemographicsField.DateOfBirth,
            DemographicsField.GenderIdentity,
            DemographicsField.Height,
            DemographicsField.Weight,
            DemographicsField.RaceEthnicity,
            DemographicsField.LatinoStatus,
        )
    }

    @Test
    fun `trial participants also need comorbidities, stage of change and education`() {
        // given
        val state = DemographicsStateFixtures.create(
            region = DemographicsRegion.UK,
            optedIntoTrial = true,
            response = DemographicsResponse.empty,
        )

        // when
        val completion = rules.completion(state = state)

        // then
        assertThat(completion.missingRequired()).containsExactly(
            DemographicsField.DateOfBirth,
            DemographicsField.GenderIdentity,
            DemographicsField.Height,
            DemographicsField.Weight,
            DemographicsField.RaceEthnicity,
            DemographicsField.Comorbidities,
            DemographicsField.StageOfChange,
            DemographicsField.UKEducation,
        )
    }

    @Test
    fun `unset values and empty selections count as unanswered`() {
        // given
        val state = DemographicsStateFixtures.create(
            response = DemographicsResponseFixtures.create(
                raceEthnicity = RaceEthnicity.NONE,
                latinoStatus = LatinoStatusOption.NOT_SET,
            ),
        )

        // when
        val completion = rules.completion(state = state)

        // then
        assertThat(completion[DemographicsField.RaceEthnicity]?.isMissingRequired).isTrue()
        assertThat(completion[DemographicsField.LatinoStatus]?.isMissingRequired).isTrue()
    }

    @Test
    fun `a fully answered form has only answered questions`() {
        // given
        val state = DemographicsStateFixtures.create(
            optedIntoTrial = true,
            response = DemographicsResponseFixtures.answered(),
        )

        // when
        val completion = rules.completion(state = state)

        // then
        assertThat(completion.values.map { it.isAnswered }.toSet()).containsExactly(true)
    }

    @Test
    fun `optional questions stay unanswered without blocking completion`() {
        // given
        val state = DemographicsStateFixtures.create(
            response = DemographicsResponseFixtures.answered().copy(
                bloodType = null,
                referralSource = null,
            ),
        )

        // when
        val completion = rules.completion(state = state)

        // then
        assertThat(completion[DemographicsField.BloodType]).isEqualTo(
            DemographicsCompletion(
                isAnswered = false,
                isRequired = false,
            ),
        )
        assertThat(completion[DemographicsField.ReferralSource]?.isMissingRequired).isFalse()
    }

    @Test
    fun `a new gender identity fills a missing sex at birth`() {
        // given
        val previous = DemographicsResponse.empty

        // when
        val inferred = rules.inferred(
            previous = previous,
            updated = previous.copy(genderIdentity = MHCGenderIdentity.TRANS_MALE),
        )

        // then
        assertThat(inferred.biologicalSexAtBirth).isEqualTo(BiologicalSex.FEMALE)
    }

    @Test
    fun `a new sex at birth fills a missing gender identity`() {
        // given
        val previous = DemographicsResponse.empty

        // when
        val inferred = rules.inferred(
            previous = previous,
            updated = previous.copy(biologicalSexAtBirth = BiologicalSex.MALE),
        )

        // then
        assertThat(inferred.genderIdentity).isEqualTo(MHCGenderIdentity.MALE)
    }

    @Test
    fun `an answered counterpart is never overwritten`() {
        // given
        val previous = DemographicsResponse.empty.copy(biologicalSexAtBirth = BiologicalSex.INTERSEX)

        // when
        val inferred = rules.inferred(
            previous = previous,
            updated = previous.copy(genderIdentity = MHCGenderIdentity.FEMALE),
        )

        // then
        assertThat(inferred.biologicalSexAtBirth).isEqualTo(BiologicalSex.INTERSEX)
    }

    @Test
    fun `gender identities without an implied sex leave it unanswered`() {
        // given
        val previous = DemographicsResponse.empty

        // when
        val inferred = rules.inferred(
            previous = previous,
            updated = previous.copy(genderIdentity = MHCGenderIdentity.OTHER),
        )

        // then
        assertThat(inferred.biologicalSexAtBirth).isNull()
    }

    private fun Map<DemographicsField, DemographicsCompletion>.missingRequired() =
        filterValues { it.isMissingRequired }.keys
}
