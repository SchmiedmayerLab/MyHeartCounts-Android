//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard

import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse

/**
 * A [QuestionnaireResponseUploader] that records every response instead of uploading it.
 */
class FakeQuestionnaireResponseUploader : QuestionnaireResponseUploader {
    private val _uploads = mutableListOf<QuestionnaireResponse>()

    /**
     * Every response passed to [upload], in order.
     */
    val uploads: List<QuestionnaireResponse> get() = _uploads

    override suspend fun upload(
        response: QuestionnaireResponse,
        questionnaire: Questionnaire,
    ): Result<Unit> {
        _uploads += response
        return Result.success(Unit)
    }
}
