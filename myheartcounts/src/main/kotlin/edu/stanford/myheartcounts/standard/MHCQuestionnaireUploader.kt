//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard

import ca.uhn.fhir.context.FhirContext
import edu.stanford.myheartcounts.firebase.MHCFirestore
import edu.stanford.myheartcounts.standard.health.FhirJson
import kotlinx.coroutines.tasks.await
import org.grovealliance.core.Module
import org.grovealliance.core.logging.groveLogger
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType
import java.util.UUID

/**
 * Uploads completed questionnaire responses to the participant's document.
 *
 * Ports `MyHeartCountsStandard+QuestionnaireResponse`, writing the same FHIR
 * `QuestionnaireResponse` documents into the same collection so responses from either platform can
 * be analyzed together.
 *
 * Note that nothing calls this yet: the app has no screen that presents a study-bundle questionnaire,
 * so no response is produced to upload. The path exists so that presenting one is the only thing
 * left to build.
 */
class MHCQuestionnaireUploader(
    private val firestore: MHCFirestore,
    private val appRevision: String,
) : Module {

    private val logger by groveLogger(tag = "MHCFirebase")
    private val fhirParser by lazy { FhirContext.forR4().newJsonParser() }

    /**
     * Uploads [response] as the participant's answer to [questionnaire].
     *
     * The questionnaire's canonical URL is stamped onto the response even when it is already set:
     * it is how the backend and the analysis tell one survey's responses from another's, and iOS
     * does the same after hitting cases where it was missing.
     *
     * @param response The participant's completed response.
     * @param questionnaire The questionnaire it answers.
     */
    suspend fun upload(
        response: QuestionnaireResponse,
        questionnaire: Questionnaire,
    ): Result<Unit> = runCatching {
        firestore.awaitReady()

        questionnaire.url?.let { response.setQuestionnaire(it) }
        response.extension.removeAll { it.url == APP_REVISION_URL }
        response.addExtension(Extension(APP_REVISION_URL, StringType(appRevision)))

        val documentId = response.identifier?.value?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()
        firestore.questionnaireResponses
            .document(documentId)
            .set(FhirJson.toFirestoreMap(json = fhirParser.encodeResourceToString(response)))
            .await()
        Unit
    }.onFailure { throwable ->
        logger.e(throwable) { "Could not store the questionnaire response." }
    }

    private companion object {
        /**
         * The app version that produced the response. Shared with iOS.
         */
        const val APP_REVISION_URL = "https://bdh.stanford.edu/fhir/defs/mhcAppRevision"
    }
}
