//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard

import ca.uhn.fhir.context.FhirContext
import edu.stanford.myheartcounts.firebase.MHCFirestore
import edu.stanford.myheartcounts.firebase.mhcFirebaseLogger
import edu.stanford.myheartcounts.standard.health.FhirJson
import kotlinx.coroutines.tasks.await
import org.grovealliance.core.Module
import org.grovealliance.core.coroutines.runCatchingAsync
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import java.util.UUID

/**
 * Uploads a participant's completed questionnaire responses.
 */
interface QuestionnaireResponseUploader : Module {
    /**
     * Uploads [response] as the participant's answer to [questionnaire].
     *
     * @param response The participant's completed response.
     * @param questionnaire The questionnaire it answers.
     */
    suspend fun upload(
        response: QuestionnaireResponse,
        questionnaire: Questionnaire,
    ): Result<Unit>
}

/**
 * Writes each response as a FHIR `QuestionnaireResponse` document into the participant's
 * questionnaire responses, carrying the questionnaire's canonical URL and the app revision.
 */
class MHCQuestionnaireUploader(
    private val firestore: MHCFirestore,
    private val appRevision: MHCAppRevision,
) : QuestionnaireResponseUploader {

    private val logger by mhcFirebaseLogger()
    private val fhirParser by lazy { FhirContext.forR4().newJsonParser() }

    /**
     * The questionnaire's canonical URL replaces any the response already carries, since it is what
     * tells one survey's responses from another's.
     */
    override suspend fun upload(
        response: QuestionnaireResponse,
        questionnaire: Questionnaire,
    ): Result<Unit> = runCatchingAsync {
        questionnaire.url?.let { response.setQuestionnaire(it) }
        response.extension.removeAll { it.url == MHCAppRevision.URL }
        response.addExtension(appRevision.toExtension())

        val documentId = response.identifier?.value?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()
        firestore.questionnaireResponses()
            .document(documentId)
            .set(FhirJson.toFirestoreMap(json = fhirParser.encodeResourceToString(response)))
            .await()
        Unit
    }.onFailure { throwable ->
        logger.e(throwable) { "Could not store the questionnaire response." }
    }
}
