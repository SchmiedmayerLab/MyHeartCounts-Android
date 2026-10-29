//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import org.grovealliance.scheduler.Event
import org.hl7.fhir.r4.model.Questionnaire

/**
 * A [StudyQuestionnaireSource] returning a configured result for every event.
 */
class FakeStudyQuestionnaireSource : StudyQuestionnaireSource {
    private var result: Result<StudyQuestionnaire> = Result.success(StudyQuestionnaire.prepare(questionnaire = Questionnaire()))

    /**
     * Sets what [questionnaire] returns.
     */
    fun setResult(result: Result<StudyQuestionnaire>) {
        this.result = result
    }

    override suspend fun questionnaire(event: Event): Result<StudyQuestionnaire> = result

    override suspend fun questionnaire(name: String): Result<StudyQuestionnaire> = result
}
