//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.consent

import android.content.Context
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import edu.stanford.myheartcounts.account.DidOptInToTrialKey
import edu.stanford.myheartcounts.account.FutureStudiesKey
import edu.stanford.myheartcounts.account.LastSignedConsentDateKey
import edu.stanford.myheartcounts.account.LastSignedConsentVersionKey
import edu.stanford.myheartcounts.firebase.MHCFirebaseLoader
import edu.stanford.myheartcounts.firebase.MHCFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.grovealliance.account.Account
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountModifications
import org.grovealliance.consent.ConsentResponses
import org.grovealliance.core.Module
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.foundation.JsonSerializer
import org.grovealliance.markdown.MarkdownDocument
import org.grovealliance.markdown.MarkdownMetadata
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Uploads the consent form a participant signed, and records that they signed it.
 *
 * The PDF is the study's record of what this participant actually agreed to, so it is stored
 * verbatim alongside the document's own metadata and the answers they gave to its interactive
 * elements. Ports `MyHeartCountsStandard+Consent`, writing to the same Cloud Storage path with the
 * same custom metadata keys.
 */
class MHCConsentUploader(
    private val context: Context,
    private val account: Account,
    private val firestore: MHCFirestore,
    private val renderer: ConsentPdfRenderer,
    private val timeProvider: TimeProvider,
) : Module {

    private val logger by groveLogger(tag = "MHCFirebase")

    /**
     * Renders [document] with [responses] to a PDF, uploads it, and stamps the consent fields onto
     * the participant's account.
     *
     * The account is only updated once the upload succeeds: a participant recorded as consented
     * whose form never arrived is worse than one asked to sign again.
     *
     * @param document The consent document, as Grove's markdown module parsed it — the same model
     * the consent screen rendered from, so its frontmatter and version are read rather than passed.
     * @param responses What the participant answered, including their signature.
     */
    suspend fun upload(
        document: MarkdownDocument,
        responses: ConsentResponses,
    ): Result<Unit> = runCatching {
        firestore.awaitReady()

        val signedAt = timeProvider.nowInstant()
        val version = document.metadata.version?.toString()
        val pdf = renderer.render(
            document = document,
            responses = responses,
            paperSize = ConsentPaperSize.forRegion(
                regionCode = MHCFirebaseLoader.persistedRegionCode(context = context),
            ),
        )

        val metadata = storageMetadata {
            contentType = PDF_CONTENT_TYPE
            setCustomMetadata(KEY_CONSENT_FORM_METADATA, encode(metadata = document.metadata))
            setCustomMetadata(KEY_RESPONSES, encode(responses = responses))
            setCustomMetadata(KEY_DATE, DATE_FORMAT.format(signedAt.atOffset(ZoneOffset.UTC)))
            version?.let { setCustomMetadata(KEY_VERSION, it) }
        }
        // The file name is the signing time in whole seconds, which is what the read-back listing
        // sorts on; iOS relies on the same convention.
        FirebaseStorage.getInstance().reference
            .child("$USERS_PREFIX/${firestore.accountId}/$CONSENT_FOLDER/${signedAt.epochSecond}.pdf")
            .putBytes(pdf, metadata)
            .await()

        recordOnAccount(responses = responses, version = version, signedAt = signedAt)
    }.onFailure { throwable ->
        logger.e(throwable) { "Failed to upload the signed consent form." }
    }

    private suspend fun recordOnAccount(
        responses: ConsentResponses,
        version: String?,
        signedAt: Instant,
    ) {
        val modifiedDetails = AccountDetails()
        modifiedDetails[LastSignedConsentDateKey::class] = signedAt
        version?.let { modifiedDetails[LastSignedConsentVersionKey::class] = it }
        responses.toggles[TOGGLE_FUTURE_STUDIES]?.let {
            modifiedDetails[FutureStudiesKey::class] = it
        }
        responses.selects[SELECT_TRIAL_OPT_IN]?.let {
            modifiedDetails[DidOptInToTrialKey::class] = it == OPTION_TRIAL_YES
        }
        AccountModifications(modifiedDetails = modifiedDetails)
            .mapCatching { account.service.updateAccountDetails(it).getOrThrow() }
            .onFailure { logger.e(it) { "Failed to record the consent on the account." } }
    }

    /**
     * The document's frontmatter, which iOS stores under the same key.
     */
    private fun encode(metadata: MarkdownMetadata): String =
        JsonSerializer.encode(value = metadata, strategy = MarkdownMetadata.serializer())

    /**
     * The participant's answers, flattened to one string per element.
     *
     * Signature strokes are deliberately left out: they are already drawn into the PDF, and
     * thousands of coordinates would not fit within Cloud Storage's metadata size limit.
     */
    private fun encode(responses: ConsentResponses): String {
        val flattened = responses.toggles.mapValues { it.value.toString() } +
            responses.selects +
            responses.signatures.mapValues { (_, signature) ->
                "${signature.givenName} ${signature.familyName}".trim()
            }
        return JsonSerializer.encode(
            value = flattened,
            strategy = MapSerializer(String.serializer(), String.serializer()),
        )
    }

    private companion object {
        const val PDF_CONTENT_TYPE = "application/pdf"
        const val USERS_PREFIX = "users"
        const val CONSENT_FOLDER = "consent"

        const val KEY_CONSENT_FORM_METADATA = "consentFormMetadata"
        const val KEY_RESPONSES = "responses"
        const val KEY_DATE = "date"
        const val KEY_VERSION = "version"

        /**
         * The consent document elements whose answers are mirrored onto the account, matching iOS's
         * `futureStudiesOptIn` and `trialOptIn`.
         */
        const val TOGGLE_FUTURE_STUDIES = "futureStudiesOptIn"
        const val SELECT_TRIAL_OPT_IN = "trialOptIn"
        const val OPTION_TRIAL_YES = "trialYes"

        /**
         * ISO-8601 in GMT with a dash date separator, matching iOS's `consentDateFormat`.
         */
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
    }
}
