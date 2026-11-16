//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.entry

import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse

/**
 * A [QuestionnaireMeasurementWriter] that records every response instead of saving its measurements.
 */
class FakeQuestionnaireMeasurementWriter : QuestionnaireMeasurementWriter {
    private val _saves = mutableListOf<QuestionnaireResponse>()

    /**
     * Every response passed to [save], in order.
     */
    val saves: List<QuestionnaireResponse> get() = _saves

    override suspend fun save(
        response: QuestionnaireResponse,
        questionnaire: Questionnaire,
    ): Result<Unit> {
        _saves += response
        return Result.success(Unit)
    }
}
