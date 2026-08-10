//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition

import com.google.common.truth.Truth.assertThat
import edu.stanford.spezi.foundation.fixtures.UUIDFixtures
import edu.stanford.spezi.studydefinition.fixtures.StudyBundleFixtures
import org.junit.Test
import java.util.Locale

class StudyBundleTest {

    private val enUs = Locale.forLanguageTag("en-US")

    private fun openExample(): StudyBundle = StudyBundle.open(StudyBundleFixtures.exampleBundleDir())

    @Test
    fun `opens the bundle and decodes its definition`() {
        // when
        val bundle = openExample()

        // then
        assertThat(bundle.id).isEqualTo(UUIDFixtures.repeating('1'))
        assertThat(bundle.studyDefinition.components).hasSize(5)
    }

    @Test
    fun `resolves a localized article`() {
        // given
        val bundle = openExample()
        val article = bundle.studyDefinition.components.filterIsInstance<Component.Informational>().single()

        // when
        val file = bundle.resolve(article.fileRef, enUs)

        // then
        assertThat(file).isNotNull()
        assertThat(file!!.name).isEqualTo("welcome+en-US.md")
    }

    @Test
    fun `falls back to the default locale for an unknown locale`() {
        // given
        val bundle = openExample()
        val article = bundle.studyDefinition.components.filterIsInstance<Component.Informational>().single()

        // when
        val file = bundle.resolve(article.fileRef, Locale.forLanguageTag("fr-FR"))

        // then
        assertThat(file).isNotNull()
        assertThat(file!!.name).isEqualTo("welcome+en-US.md")
    }

    @Test
    fun `derives display title and subtitle from resources`() {
        // given
        val bundle = openExample()
        val components = bundle.studyDefinition.components

        // when / then
        val informational = components.filterIsInstance<Component.Informational>().single()
        assertThat(bundle.displayTitle(informational, enUs)).isEqualTo("Welcome to the Example Study")
        assertThat(bundle.displaySubtitle(informational, enUs))
            .isEqualTo("Learn what taking part in this study involves.")

        // questionnaire reads FHIR title/purpose
        val questionnaire = components.filterIsInstance<Component.Questionnaire>().single()
        assertThat(bundle.displayTitle(questionnaire, enUs)).isEqualTo("Daily Check-In")
        assertThat(bundle.displaySubtitle(questionnaire, enUs))
            .isEqualTo("A short daily check-in about how you're feeling.")

        // timed walking test derives its title
        val walk = components.filterIsInstance<Component.TimedWalkingTest>().single()
        assertThat(bundle.displayTitle(walk, enUs)).isEqualTo("6-Minute Walk Test")

        // health data collection has no title
        val health = components.filterIsInstance<Component.HealthDataCollection>().single()
        assertThat(bundle.displayTitle(health, enUs)).isNull()
    }

    @Test
    fun `reads the consent text`() {
        // when
        val consent = openExample().consentText(enUs)

        // then
        assertThat(consent).isNotNull()
        assertThat(consent).contains("Consent to Participate")
    }
}
