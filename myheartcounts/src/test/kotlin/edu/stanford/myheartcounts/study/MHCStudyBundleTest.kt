//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import ca.uhn.fhir.context.FhirContext
import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.BuildConfig
import edu.stanford.myheartcounts.standard.consent.MHCConsentDocumentProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.grovealliance.markdown.MarkdownBlock
import org.grovealliance.markdown.MarkdownDocument
import org.grovealliance.studydefinition.Component
import org.grovealliance.studydefinition.FileReference
import org.grovealliance.studydefinition.StudyBundle
import org.grovealliance.studydefinition.StudyDefinitionJson
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Questionnaire
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Locale

class MHCStudyBundleTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val archive: File = File(GENERATED_ROOT)
        .walkTopDown()
        .firstOrNull { it.isFile && it.name == BuildConfig.STUDY_BUNDLE_ASSET_PATH }
        ?: error("No study bundle under $GENERATED_ROOT — run ./gradlew :myheartcounts:exportStudyBundle")

    private val bundleDir: File by lazy {
        val target = File(temporaryFolder.root, archive.name.removeSuffix(ARCHIVE_SUFFIX))
        archive.inputStream().use { StudyBundle.unpack(archive = it, bundleDir = target) }
    }

    private val bundle: StudyBundle by lazy { StudyBundle.open(bundleDir) }
    private val fhirParser by lazy { FhirContext.forR4().newJsonParser() }

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
    fun `exports the schema version the definition model decodes`() {
        // when
        val schemaVersion = Json.parseToJsonElement(File(bundleDir, DEFINITION_FILENAME).readText())
            .jsonObject[SCHEMA_VERSION_KEY]
            ?.jsonPrimitive
            ?.content

        // then
        assertThat(schemaVersion).isEqualTo(StudyDefinitionJson.SCHEMA_VERSION)
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

    @Test
    fun `declares the consent elements mirrored onto the account in every localization`() {
        // given
        val locales = listOf(Locale.US, Locale.forLanguageTag("es-US"))

        // when
        val elementIds = locales.map { locale ->
            consentElementIds(text = requireNotNull(bundle.consentText(locale = locale)))
        }

        // then
        elementIds.forEach { ids ->
            assertThat(ids).containsAtLeast(
                MHCConsentDocumentProvider.ID_FUTURE_STUDIES,
                MHCConsentDocumentProvider.ID_TRIAL_OPT_IN,
                MHCConsentDocumentProvider.ID_TRIAL_YES,
            )
        }
    }

    @Test
    fun `prepares every shipped questionnaire into one the renderer fully supports`() {
        // given
        val locales = listOf(Locale.US, Locale.forLanguageTag("es-US"))
        val fileRefs = bundle.studyDefinition.components
            .filterIsInstance<Component.Questionnaire>()
            .map { it.fileRef }

        // when
        val prepared = fileRefs.flatMap { fileRef ->
            locales.map { locale ->
                val text = requireNotNull(bundle.resolve(fileRef, locale)).readText()
                StudyQuestionnaire.prepare(questionnaire = fhirParser.parseResource(Questionnaire::class.java, text))
            }
        }

        // then
        assertThat(prepared).hasSize(fileRefs.size * locales.size)
        prepared.forEach { questionnaire ->
            assertThat(questionnaire.questionnaire.url).isNotEmpty()
            val items = questionnaire.questionnaire.item.flattened()
            assertThat(items.map { it.type }).doesNotContain(Questionnaire.QuestionnaireItemType.QUANTITY)
            items.forEach { item ->
                item.extension
                    .filter { it.url in StudyQuestionnaire.BOUND_EXTENSION_URLS }
                    .forEach { bound -> assertThat(bound.value.fhirType()).isEqualTo(item.type.toCode()) }
                (item.getExtensionByUrl(ITEM_CONTROL_URL)?.value as? CodeableConcept)?.coding?.forEach { coding ->
                    assertThat(coding.system).isEqualTo(ITEM_CONTROL_SYSTEM)
                    if (coding.code == CHECK_BOX_CODE) assertThat(item.repeats).isTrue()
                }
            }
        }
    }

    private fun List<Questionnaire.QuestionnaireItemComponent>.flattened(): List<Questionnaire.QuestionnaireItemComponent> =
        flatMap { listOf(it) + it.item.flattened() }

    /**
     * The ids of every consent element in [text], including options nested inside selects.
     */
    private fun consentElementIds(text: String): Set<String> {
        val document = MarkdownDocument.process(
            text = text,
            customElementNames = MHCConsentDocumentProvider.CONSENT_ELEMENT_NAMES,
        )
        return document.blocks
            .filterIsInstance<MarkdownBlock.Element>()
            .flatMap { element ->
                listOf(element) + element.content
                    .filterIsInstance<MarkdownBlock.Element.Content.Element>()
                    .map { it.element }
            }
            .mapNotNull { it.id }
            .toSet()
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
        /**
         * Root the build exports the bundle archive beneath, relative to the module directory.
         */
        const val GENERATED_ROOT = "build/generated"
        const val ARCHIVE_SUFFIX = ".tar.zst"
        const val DEFINITION_FILENAME = "definition.json"
        const val SCHEMA_VERSION_KEY = "schemaVersion"
        const val ITEM_CONTROL_URL = "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl"
        const val ITEM_CONTROL_SYSTEM = "http://hl7.org/fhir/questionnaire-item-control"
        const val CHECK_BOX_CODE = "check-box"

        /**
         * The oldest revision known to decode. Raise this only alongside a bundle update.
         */
        const val MINIMUM_REVISION = 42L
    }
}
