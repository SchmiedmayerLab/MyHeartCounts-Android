//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import com.google.common.truth.Truth.assertThat
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DecimalType
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Test

class StudyQuestionnaireTest {

    @Test
    fun `renders a quantity question with a single unit as a decimal question`() {
        // given
        val questionnaire = questionnaire()

        // when
        val prepared = StudyQuestionnaire.prepare(questionnaire = questionnaire)

        // then
        val item = prepared.questionnaire.item.single().item.single { it.linkId == QUANTITY_LINK_ID }
        assertThat(item.type).isEqualTo(QuestionnaireItemType.DECIMAL)
        assertThat((item.getExtensionByUrl(StudyQuestionnaire.UNIT_EXTENSION_URL).value as Coding).code)
            .isEqualTo(UNIT_CODE)
    }

    @Test
    fun `renders the bounds of a decimal question as decimals`() {
        // given
        val questionnaire = questionnaire()

        // when
        val prepared = StudyQuestionnaire.prepare(questionnaire = questionnaire)

        // then
        val item = prepared.questionnaire.item.single().item.single { it.linkId == QUANTITY_LINK_ID }
        val bounds = item.extension.filter { it.url in StudyQuestionnaire.BOUND_EXTENSION_URLS }
        assertThat(bounds.map { (it.value as DecimalType).value.toInt() }).containsExactly(MIN, MAX)
    }

    @Test
    fun `leaves the original questionnaire unchanged`() {
        // given
        val questionnaire = questionnaire()

        // when
        StudyQuestionnaire.prepare(questionnaire = questionnaire)

        // then
        val item = questionnaire.item.single().item.single { it.linkId == QUANTITY_LINK_ID }
        assertThat(item.type).isEqualTo(QuestionnaireItemType.QUANTITY)
        assertThat(item.getExtensionByUrl(MIN_VALUE_URL).value).isInstanceOf(IntegerType::class.java)
    }

    @Test
    fun `codes item controls in the item control code system`() {
        // given
        val questionnaire = questionnaire()

        // when
        val prepared = StudyQuestionnaire.prepare(questionnaire = questionnaire)

        // then
        val control = prepared.questionnaire.item.single().getExtensionByUrl(ITEM_CONTROL_URL).value as CodeableConcept
        assertThat(control.coding.single().system).isEqualTo(ITEM_CONTROL_SYSTEM)
        assertThat(control.coding.single().code).isEqualTo(PAGE_CODE)
    }

    @Test
    fun `lets a check-box question take several answers`() {
        // given
        val questionnaire = questionnaire()

        // when
        val prepared = StudyQuestionnaire.prepare(questionnaire = questionnaire)

        // then
        val item = prepared.questionnaire.item.single().item.single { it.linkId == CHOICE_LINK_ID }
        assertThat(item.repeats).isTrue()
    }

    @Test
    fun `restores the answer to a quantity question as a quantity in its unit`() {
        // given
        val prepared = StudyQuestionnaire.prepare(questionnaire = questionnaire())
        val rendered = renderedResponse()

        // when
        val response = prepared.responseFor(rendered = rendered)

        // then
        val answer = response.item.single().item.single { it.linkId == QUANTITY_LINK_ID }.answer.single()
        assertThat(answer.valueQuantity.value.toInt()).isEqualTo(ANSWER)
        assertThat(answer.valueQuantity.unit).isEqualTo(UNIT_CODE)
    }

    @Test
    fun `marks the response completed`() {
        // given
        val prepared = StudyQuestionnaire.prepare(questionnaire = questionnaire())

        // when
        val response = prepared.responseFor(rendered = renderedResponse())

        // then
        assertThat(response.status).isEqualTo(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED)
    }

    @Test
    fun `keeps the answers to every other question`() {
        // given
        val prepared = StudyQuestionnaire.prepare(questionnaire = questionnaire())

        // when
        val response = prepared.responseFor(rendered = renderedResponse())

        // then
        val answer = response.item.single().item.single { it.linkId == BOOLEAN_LINK_ID }.answer.single()
        assertThat(answer.valueBooleanType.booleanValue()).isTrue()
    }

    private fun questionnaire() = Questionnaire().apply {
        url = "https://example.org/fhir/survey/sample"
        addItem().apply {
            linkId = GROUP_LINK_ID
            type = QuestionnaireItemType.GROUP
            addExtension(
                Extension(
                    ITEM_CONTROL_URL,
                    CodeableConcept().addCoding(Coding().setSystem(ITEM_CONTROL_VALUE_SET).setCode(PAGE_CODE)),
                )
            )
            addItem().apply {
                linkId = QUANTITY_LINK_ID
                type = QuestionnaireItemType.QUANTITY
                addExtension(Extension(StudyQuestionnaire.UNIT_EXTENSION_URL, Coding().setCode(UNIT_CODE)))
                addExtension(Extension(MIN_VALUE_URL, IntegerType(MIN)))
                addExtension(Extension(MAX_VALUE_URL, IntegerType(MAX)))
            }
            addItem().apply {
                linkId = BOOLEAN_LINK_ID
                type = QuestionnaireItemType.BOOLEAN
            }
            addItem().apply {
                linkId = CHOICE_LINK_ID
                type = QuestionnaireItemType.CHOICE
                addExtension(
                    Extension(
                        ITEM_CONTROL_URL,
                        CodeableConcept().addCoding(Coding().setSystem(ITEM_CONTROL_VALUE_SET).setCode(CHECK_BOX_CODE)),
                    )
                )
            }
        }
    }

    private fun renderedResponse() = QuestionnaireResponse().apply {
        addItem().apply {
            linkId = GROUP_LINK_ID
            addItem().apply {
                linkId = QUANTITY_LINK_ID
                addAnswer().value = DecimalType(ANSWER.toLong())
            }
            addItem().apply {
                linkId = BOOLEAN_LINK_ID
                addAnswer().value = BooleanType(true)
            }
        }
    }

    private companion object {
        const val GROUP_LINK_ID = "group"
        const val QUANTITY_LINK_ID = "systolic"
        const val BOOLEAN_LINK_ID = "smoker"
        const val UNIT_CODE = "mm[Hg]"
        const val MIN = 60
        const val MAX = 250
        const val ANSWER = 120
        const val MIN_VALUE_URL = "http://hl7.org/fhir/StructureDefinition/minValue"
        const val MAX_VALUE_URL = "http://hl7.org/fhir/StructureDefinition/maxValue"
        const val ITEM_CONTROL_URL = "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl"
        const val ITEM_CONTROL_SYSTEM = "http://hl7.org/fhir/questionnaire-item-control"
        const val ITEM_CONTROL_VALUE_SET = "http://hl7.org/fhir/ValueSet/questionnaire-item-control"
        const val PAGE_CODE = "page"
        const val CHECK_BOX_CODE = "check-box"
        const val CHOICE_LINK_ID = "medications"
    }
}
