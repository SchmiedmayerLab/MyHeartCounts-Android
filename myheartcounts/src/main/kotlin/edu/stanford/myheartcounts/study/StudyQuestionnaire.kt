//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DecimalType
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.Quantity
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse

/**
 * A study questionnaire prepared for the questionnaire renderer, together with what is needed to turn
 * the renderer's response back into one that answers the original [Questionnaire].
 *
 * The renderer records an answer to a quantity question only once a unit is picked from the
 * question's unit options. A question with a single unit, whether fixed or the only unit option, is
 * therefore rendered as a decimal question, showing its unit beside the input, and [responseFor]
 * restores its answers as quantities in that unit. Numeric bounds, including quantity bounds, are
 * rendered as decimals, the type the renderer compares a decimal answer against.
 *
 * Item controls, such as pages and check boxes, take effect only when coded in the item control code
 * system; codes given under its value set are moved to it. A check-box question accepts several
 * answers.
 */
class StudyQuestionnaire private constructor(
    /**
     * The questionnaire to render.
     */
    val questionnaire: Questionnaire,
    private val quantityUnits: Map<String, Coding>,
) {

    /**
     * The response to upload for [rendered], the response the renderer produced for [questionnaire]
     * once the participant submitted it: it is marked completed, and every answer to a question
     * rendered as a decimal becomes a quantity in the question's unit.
     */
    fun responseFor(rendered: QuestionnaireResponse): QuestionnaireResponse = rendered.copy().apply {
        status = QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED
        restoreQuantities(items = item)
    }

    private fun restoreQuantities(items: List<QuestionnaireResponse.QuestionnaireResponseItemComponent>) {
        items.forEach { item ->
            val unit = quantityUnits[item.linkId]
            item.answer.forEach { answer ->
                if (unit != null && answer.hasValueDecimalType()) {
                    answer.value = Quantity()
                        .setValue(answer.valueDecimalType.value)
                        .setUnit(unit.code)
                }
                restoreQuantities(items = answer.item)
            }
            restoreQuantities(items = item.item)
        }
    }

    companion object {
        /**
         * The extension giving a quantity question its single unit.
         */
        const val UNIT_EXTENSION_URL = "http://hl7.org/fhir/StructureDefinition/questionnaire-unit"

        /**
         * The extension offering a unit a quantity question can be answered in.
         */
        const val UNIT_OPTION_EXTENSION_URL = "http://hl7.org/fhir/StructureDefinition/questionnaire-unitOption"

        /**
         * The extensions bounding a quantity answer, each with the bound for a numeric answer it is
         * rendered as, in the same order as [BOUND_EXTENSION_URLS].
         */
        val QUANTITY_BOUND_EXTENSION_URLS = listOf(
            "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-minQuantity",
            "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-maxQuantity",
        )

        /**
         * The extensions bounding a numeric answer.
         */
        val BOUND_EXTENSION_URLS = listOf(
            "http://hl7.org/fhir/StructureDefinition/minValue",
            "http://hl7.org/fhir/StructureDefinition/maxValue",
        )

        private const val ITEM_CONTROL_EXTENSION_URL =
            "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl"
        private const val ITEM_CONTROL_SYSTEM = "http://hl7.org/fhir/questionnaire-item-control"
        private const val ITEM_CONTROL_VALUE_SET = "http://hl7.org/fhir/ValueSet/questionnaire-item-control"
        private const val CHECK_BOX_CONTROL = "check-box"

        /**
         * Prepares [questionnaire] for rendering; [questionnaire] itself is left unchanged.
         */
        fun prepare(questionnaire: Questionnaire): StudyQuestionnaire {
            val renderable = questionnaire.copy()
            val quantityUnits = mutableMapOf<String, Coding>()
            prepareItems(items = renderable.item, quantityUnits = quantityUnits)
            return StudyQuestionnaire(
                questionnaire = renderable,
                quantityUnits = quantityUnits,
            )
        }

        private fun prepareItems(
            items: List<Questionnaire.QuestionnaireItemComponent>,
            quantityUnits: MutableMap<String, Coding>,
        ) {
            items.forEach { item ->
                val controls = (item.getExtensionByUrl(ITEM_CONTROL_EXTENSION_URL)?.value as? CodeableConcept)
                    ?.coding
                    .orEmpty()
                controls
                    .filter { it.system == ITEM_CONTROL_VALUE_SET }
                    .forEach { it.system = ITEM_CONTROL_SYSTEM }
                if (controls.any { it.code == CHECK_BOX_CONTROL }) item.repeats = true
                val unit = singleUnit(item = item)
                if (item.type == Questionnaire.QuestionnaireItemType.QUANTITY && unit != null) {
                    item.type = Questionnaire.QuestionnaireItemType.DECIMAL
                    quantityUnits[item.linkId] = unit
                    item.removeExtension(UNIT_OPTION_EXTENSION_URL)
                    if (!item.hasExtension(UNIT_EXTENSION_URL)) item.addExtension(UNIT_EXTENSION_URL, unit)
                    renderQuantityBoundsAsDecimals(item = item)
                }
                if (item.type == Questionnaire.QuestionnaireItemType.DECIMAL) {
                    item.extension
                        .filter { it.url in BOUND_EXTENSION_URLS }
                        .forEach { bound ->
                            (bound.value as? IntegerType)?.let { bound.setValue(DecimalType(it.value.toLong())) }
                        }
                }
                prepareItems(items = item.item, quantityUnits = quantityUnits)
            }
        }

        private fun singleUnit(item: Questionnaire.QuestionnaireItemComponent): Coding? =
            item.getExtensionByUrl(UNIT_EXTENSION_URL)?.value as? Coding
                ?: item.getExtensionsByUrl(UNIT_OPTION_EXTENSION_URL)
                    .map { it.value as? Coding }
                    .singleOrNull()

        private fun renderQuantityBoundsAsDecimals(item: Questionnaire.QuestionnaireItemComponent) {
            QUANTITY_BOUND_EXTENSION_URLS.zip(BOUND_EXTENSION_URLS).forEach { (quantityUrl, decimalUrl) ->
                val bound = item.getExtensionByUrl(quantityUrl)?.value as? Quantity ?: return@forEach
                item.removeExtension(quantityUrl)
                item.addExtension(decimalUrl, DecimalType(bound.value))
            }
        }
    }
}
