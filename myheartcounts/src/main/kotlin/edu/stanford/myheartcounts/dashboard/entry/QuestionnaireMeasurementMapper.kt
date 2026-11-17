//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.entry

import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import org.grovealliance.core.time.TimeProvider
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.Quantity
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import java.time.Instant
import java.util.UUID

/**
 * Maps a completed questionnaire response to the measurements its questionnaire marks for
 * extraction, dated when the response was authored.
 *
 * A question marked for extraction carries the code of its measurement: a fasting blood glucose
 * (LOINC 2339-0, in mg/dL), or a blood pressure panel (LOINC 85354-9) whose systolic (8480-6) and
 * diastolic (8462-4) questions, in mm[Hg], are marked as its components. A marked question left
 * unanswered reports nothing.
 */
interface QuestionnaireMeasurementMapper {

    /**
     * The measurements [response] to [questionnaire] reports. Fails, reporting none of them, when an
     * answer contradicts its measurement: answered in another unit or more than once, or a blood
     * pressure panel answered in part. A marked measurement that cannot be recorded is left out.
     */
    fun mapEntries(
        response: QuestionnaireResponse,
        questionnaire: Questionnaire,
    ): Result<List<HeartScoreEntry>>
}

/**
 * Default [QuestionnaireMeasurementMapper] implementation, dating responses without an authored time
 * at the current time of [timeProvider].
 */
class QuestionnaireMeasurementMapperImpl(
    private val timeProvider: TimeProvider,
) : QuestionnaireMeasurementMapper {

    override fun mapEntries(
        response: QuestionnaireResponse,
        questionnaire: Questionnaire,
    ): Result<List<HeartScoreEntry>> = runCatching {
        val answers = response.item.flatMap { it.withNested() }.groupBy { it.linkId }
        val date = response.authored?.toInstant() ?: timeProvider.nowInstant()
        questionnaire.item
            .flatMap { it.withNested() }
            .filter { it.marking() == MARKING_MEASUREMENT }
            .mapNotNull { item ->
                when (item.loincCode()) {
                    GLUCOSE_CODE -> answers.quantity(linkId = item.linkId, unit = GLUCOSE_UNIT)?.let {
                        HeartScoreEntry.Quantity(
                            id = UUID.randomUUID().toString(),
                            date = date,
                            component = HeartScoreComponent.BloodGlucose,
                            value = it,
                        )
                    }
                    BLOOD_PRESSURE_CODE -> bloodPressure(
                        panel = item,
                        answers = answers,
                        date = date,
                    )
                    else -> null
                }
            }
    }

    private fun bloodPressure(
        panel: Questionnaire.QuestionnaireItemComponent,
        answers: Map<String, List<QuestionnaireResponse.QuestionnaireResponseItemComponent>>,
        date: Instant,
    ): HeartScoreEntry.Pressure? {
        val components = panel.item
            .filter { it.marking() == MARKING_COMPONENT }
            .associate { it.loincCode() to answers.quantity(linkId = it.linkId, unit = PRESSURE_UNIT) }
        val systolic = components[SYSTOLIC_CODE]
        val diastolic = components[DIASTOLIC_CODE]
        if (systolic == null && diastolic == null) return null
        require(systolic != null && diastolic != null) { "The blood pressure panel '${panel.linkId}' is answered in part." }
        return HeartScoreEntry.Pressure(
            id = UUID.randomUUID().toString(),
            date = date,
            systolic = systolic,
            diastolic = diastolic,
        )
    }

    private fun Map<String, List<QuestionnaireResponse.QuestionnaireResponseItemComponent>>.quantity(
        linkId: String,
        unit: String,
    ): Double? {
        val answers = get(linkId).orEmpty().flatMap { it.answer }
        require(answers.size <= 1) { "The question '$linkId' is answered more than once." }
        val quantity = answers.singleOrNull()?.value as? Quantity ?: return null
        val answeredUnit = quantity.code ?: quantity.unit
        require(answeredUnit == unit) { "The question '$linkId' is answered in '$answeredUnit' rather than '$unit'." }
        return quantity.value?.toDouble()
    }

    private fun QuestionnaireResponse.QuestionnaireResponseItemComponent.withNested():
        List<QuestionnaireResponse.QuestionnaireResponseItemComponent> =
        listOf(this) + item.flatMap { it.withNested() } + answer.flatMap { answer -> answer.item.flatMap { it.withNested() } }

    private fun Questionnaire.QuestionnaireItemComponent.withNested(): List<Questionnaire.QuestionnaireItemComponent> =
        listOf(this) + item.flatMap { it.withNested() }

    private fun Questionnaire.QuestionnaireItemComponent.marking(): String? =
        when (val value = getExtensionByUrl(OBSERVATION_EXTRACT_URL)?.value) {
            is BooleanType -> if (value.booleanValue()) MARKING_MEASUREMENT else null
            is CodeType -> value.value
            else -> null
        }

    private fun Questionnaire.QuestionnaireItemComponent.loincCode(): String? =
        code.firstOrNull { it.system == LOINC }?.code

    private companion object {
        const val OBSERVATION_EXTRACT_URL = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-observationExtract"
        const val MARKING_MEASUREMENT = "measurement"
        const val MARKING_COMPONENT = "component"
        const val LOINC = "http://loinc.org"
        const val GLUCOSE_CODE = "2339-0"
        const val BLOOD_PRESSURE_CODE = "85354-9"
        const val SYSTOLIC_CODE = "8480-6"
        const val DIASTOLIC_CODE = "8462-4"
        const val GLUCOSE_UNIT = "mg/dL"
        const val PRESSURE_UNIT = "mm[Hg]"
    }
}
