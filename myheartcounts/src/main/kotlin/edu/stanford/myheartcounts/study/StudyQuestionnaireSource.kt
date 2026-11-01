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
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.scheduler.Event
import org.grovealliance.study.ScheduledTaskAction
import org.grovealliance.study.ScheduledTaskActionKey
import org.grovealliance.studydefinition.FileReference
import org.hl7.fhir.r4.model.Questionnaire

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

    /**
     * A named questionnaire from the current study bundle.
     */
    suspend fun questionnaire(name: String): Result<StudyQuestionnaire>
}

/**
 * Reads questionnaires from the study bundle, resolving each in the [StudyVariantProvider.contentLocale].
 */
class StudyQuestionnaireSourceImpl(
    private val studyBundleProvider: MHCStudyBundleProvider,
    private val variantProvider: StudyVariantProvider,
    private val ioDispatcher: CoroutineDispatcher,
) : StudyQuestionnaireSource {

    private val fhirParser by lazy { FhirContext.forR4().newJsonParser() }

    override suspend fun questionnaire(event: Event): Result<StudyQuestionnaire> = runCatchingAsync {
        val action = event.task.context[ScheduledTaskActionKey]
        require(action is ScheduledTaskAction.AnswerQuestionnaire) {
            "The task ${event.task.id} does not present a questionnaire."
        }
        read(fileRef = action.component.fileRef)
    }

    override suspend fun questionnaire(name: String): Result<StudyQuestionnaire> = runCatchingAsync {
        read(fileRef = FileReference(category = FileReference.Category.questionnaire, filename = name, fileExtension = "json"))
    }

    private suspend fun read(fileRef: FileReference): StudyQuestionnaire {
        val bundle = studyBundleProvider.get().getOrThrow()
        return withContext(ioDispatcher) {
            val file = bundle.resolve(
                fileRef = fileRef,
                locale = variantProvider.contentLocale(),
            )
            val text = requireNotNull(file?.takeIf { it.exists() }) {
                "The study bundle holds no questionnaire at $fileRef."
            }.readText()
            StudyQuestionnaire.prepare(
                questionnaire = fhirParser.parseResource(Questionnaire::class.java, text),
            )
        }
    }
}
