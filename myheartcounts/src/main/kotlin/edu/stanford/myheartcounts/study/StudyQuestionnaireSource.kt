//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import ca.uhn.fhir.context.FhirContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.grovealliance.scheduler.Event
import org.grovealliance.study.ScheduledTaskAction
import org.grovealliance.study.ScheduledTaskActionKey
import org.hl7.fhir.r4.model.Questionnaire
import java.util.Locale

/**
 * Supplies the questionnaire a participant is asked to answer.
 */
interface StudyQuestionnaireSource {
    /**
     * The questionnaire [event] asks the participant to answer.
     *
     * Fails when the event carries no questionnaire, or when the study bundle does not hold one for
     * it.
     */
    suspend fun questionnaire(event: Event): Result<StudyQuestionnaire>
}

/**
 * Reads questionnaires from the study bundle, resolving each in the device's locale.
 */
class StudyQuestionnaireSourceImpl(
    private val studyBundleProvider: MHCStudyBundleProvider,
    private val ioDispatcher: CoroutineDispatcher,
) : StudyQuestionnaireSource {

    private val fhirParser by lazy { FhirContext.forR4().newJsonParser() }

    override suspend fun questionnaire(event: Event): Result<StudyQuestionnaire> = runCatching {
        val action = event.task.context[ScheduledTaskActionKey]
        require(action is ScheduledTaskAction.AnswerQuestionnaire) {
            "The task ${event.task.id} does not present a questionnaire."
        }
        val bundle = studyBundleProvider.get().getOrThrow()
        withContext(ioDispatcher) {
            val file = bundle.resolve(
                fileRef = action.component.fileRef,
                locale = Locale.getDefault(),
            )
            val text = requireNotNull(file?.takeIf { it.exists() }) {
                "The study bundle holds no questionnaire for the task ${event.task.id}."
            }.readText()
            StudyQuestionnaire.prepare(
                questionnaire = fhirParser.parseResource(Questionnaire::class.java, text),
            )
        }
    }
}
