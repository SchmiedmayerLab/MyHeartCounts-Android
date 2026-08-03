//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import com.google.common.truth.Truth.assertThat
import edu.stanford.spezi.studydefinition.Component
import edu.stanford.spezi.studydefinition.FileReference
import edu.stanford.spezi.studydefinition.StudyBundle
import org.junit.Test
import java.io.File
import java.util.Locale

class MHCStudyBundleTest {

    private val bundle: StudyBundle = StudyBundle.open(
        File(BUNDLE_PATH).also {
            check(it.isDirectory) { "Missing shipped study bundle at $BUNDLE_PATH" }
        }
    )

    @Test
    fun `decodes the shipped bundle`() {
        // given
        val definition = bundle.studyDefinition

        // then
        assertThat(definition.studyRevision.toLong()).isAtLeast(MINIMUM_REVISION)
        assertThat(definition.components).isNotEmpty()
        assertThat(definition.componentSchedules).isNotEmpty()
    }

    @Test
    fun `resolves every file the definition references`() {
        // when
        val unresolved = referencedFiles().filter { ref ->
            bundle.resolve(ref, Locale.US)?.exists() != true
        }

        // then
        assertThat(unresolved).isEmpty()
    }

    @Test
    fun `every scheduled component exists in the definition`() {
        // when
        val definition = bundle.studyDefinition
        val danglingComponentIds = definition.componentSchedules
            .map { it.componentId }
            .filter { definition.component(it) == null }

        // then
        assertThat(danglingComponentIds).isEmpty()
    }

    @Test
    fun `resolves the consent document the app falls back to`() {
        // given
        val fallback = File("src/main/assets/$FALLBACK_CONSENT_ASSET_PATH")

        // then
        assertThat(fallback.isFile).isTrue()
    }

    @Test
    fun `resolves the consent text for the participant's locale`() {
        // when
        val english = bundle.consentText(Locale.US)
        val spanish = bundle.consentText(Locale.forLanguageTag("es-US"))

        // then
        assertThat(english).contains("CONSENT TO BE PART OF A RESEARCH STUDY")
        assertThat(spanish).contains("FORMULARIO DE CONSENTIMIENTO DE STANFORD")
    }

    @Test
    fun `falls back to the default localization for an unsupported locale`() {
        // when
        val japanese = bundle.consentText(Locale.JAPAN)

        // then
        assertThat(japanese).isEqualTo(bundle.consentText(Locale.US))
    }

    /**
     * Every file the definition points at, across its metadata and components.
     */
    private fun referencedFiles(): List<FileReference> {
        val definition = bundle.studyDefinition
        return listOfNotNull(definition.metadata.consentFileRef) +
            definition.components.mapNotNull { component ->
                when (component) {
                    is Component.Informational -> component.fileRef
                    is Component.Questionnaire -> component.fileRef
                    is Component.HealthDataCollection,
                    is Component.TimedWalkingTest,
                    is Component.CustomActiveTask,
                    -> null
                }
            }
    }

    private companion object {
        const val BUNDLE_PATH = "src/main/assets/$STUDY_BUNDLE_ASSET_PATH"
        const val FALLBACK_CONSENT_ASSET_PATH = "$STUDY_BUNDLE_ASSET_PATH/consent/Consent+en-US.md"

        /**
         * The oldest revision known to decode. Raise this only alongside a bundle update.
         */
        const val MINIMUM_REVISION = 40L
    }
}
