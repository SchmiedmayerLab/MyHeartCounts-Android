//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ParticipationCriterionTest {

    private fun environment(
        age: Int? = 30,
        region: String? = "US",
        language: String = "en",
        enabledCustomKeys: Set<CustomCriterionKey> = emptySet(),
    ) = EvaluationEnvironment(
        age = age,
        region = region,
        language = language,
        enabledCustomKeys = enabledCustomKeys,
    )

    @Test
    fun `ageAtLeast is satisfied when old enough`() {
        assertThat(ParticipationCriterion.AgeAtLeast(18).evaluate(environment(age = 18))).isTrue()
        assertThat(ParticipationCriterion.AgeAtLeast(18).evaluate(environment(age = 17))).isFalse()
        assertThat(ParticipationCriterion.AgeAtLeast(18).evaluate(environment(age = null))).isFalse()
    }

    @Test
    fun `region and language leaves compare directly`() {
        assertThat(ParticipationCriterion.IsFromRegion("US").evaluate(environment(region = "US"))).isTrue()
        assertThat(ParticipationCriterion.IsFromRegion("US").evaluate(environment(region = "DE"))).isFalse()
        assertThat(ParticipationCriterion.SpeaksLanguage("en").evaluate(environment(language = "en"))).isTrue()
        assertThat(ParticipationCriterion.SpeaksLanguage("en").evaluate(environment(language = "de"))).isFalse()
    }

    @Test
    fun `custom criterion checks enabled keys`() {
        val key = CustomCriterionKey(
            keyValue = "invited",
            displayTitle = "Invited",
        )
        assertThat(ParticipationCriterion.Custom(key).evaluate(environment(enabledCustomKeys = setOf(key)))).isTrue()
        assertThat(ParticipationCriterion.Custom(key).evaluate(environment())).isFalse()
    }

    @Test
    fun `empty all is true and empty any is false`() {
        assertThat(ParticipationCriterion.All(emptyList()).evaluate(environment())).isTrue()
        assertThat(ParticipationCriterion.Any(emptyList()).evaluate(environment())).isFalse()
    }

    @Test
    fun `combinators compose leaf results`() {
        // given
        val criterion = ParticipationCriterion.All(
            listOf(
                ParticipationCriterion.AgeAtLeast(18),
                ParticipationCriterion.Not(ParticipationCriterion.IsFromRegion("DE")),
                ParticipationCriterion.Any(
                    listOf(
                        ParticipationCriterion.SpeaksLanguage("en"),
                        ParticipationCriterion.SpeaksLanguage("fr"),
                    ),
                ),
            ),
        )

        // then
        assertThat(criterion.evaluate(environment(age = 40, region = "US", language = "en"))).isTrue()
        assertThat(criterion.evaluate(environment(age = 40, region = "DE", language = "en"))).isFalse()
        assertThat(criterion.evaluate(environment(age = 12, region = "US", language = "en"))).isFalse()
        assertThat(criterion.evaluate(environment(age = 40, region = "US", language = "de"))).isFalse()
    }
}
