//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.entry

import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.core.logging.groveLogger
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse

/**
 * Saves the measurements a completed questionnaire response reports, so they reach the participant's
 * health data and the dashboard.
 */
interface QuestionnaireMeasurementWriter {

    /**
     * Saves the measurements [response] to [questionnaire] reports. Nothing is saved when the response
     * contradicts a measurement; otherwise each measurement is attempted, and the result fails when
     * any of them could not be saved.
     */
    suspend fun save(
        response: QuestionnaireResponse,
        questionnaire: Questionnaire,
    ): Result<Unit>
}

/**
 * Default [QuestionnaireMeasurementWriter] implementation, mapping through [mapper] and saving through
 * [entryWriter].
 */
class QuestionnaireMeasurementWriterImpl(
    private val mapper: QuestionnaireMeasurementMapper,
    private val entryWriter: HeartScoreEntryWriter,
) : QuestionnaireMeasurementWriter {

    private val logger by groveLogger()

    override suspend fun save(
        response: QuestionnaireResponse,
        questionnaire: Questionnaire,
    ): Result<Unit> = runCatchingAsync {
        val entries = mapper.mapEntries(
            response = response,
            questionnaire = questionnaire,
        ).onFailure { logger.e(it) { "Could not extract the measurements of a questionnaire response." } }
            .getOrThrow()
        val failures = entries.mapNotNull { entry -> entryWriter.save(entry = entry).exceptionOrNull() }
        failures.forEach { logger.e(it) { "Could not save a measurement extracted from a questionnaire response." } }
        failures.firstOrNull()?.let { throw it }
    }
}
