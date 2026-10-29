//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.consent

import android.content.Context
import androidx.core.content.FileProvider
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import edu.stanford.myheartcounts.account.DidOptInToTrialKey
import edu.stanford.myheartcounts.account.FutureStudiesKey
import edu.stanford.myheartcounts.account.LastSignedConsentDateKey
import edu.stanford.myheartcounts.account.LastSignedConsentVersionKey
import edu.stanford.myheartcounts.firebase.FirebaseSchema
import edu.stanford.myheartcounts.firebase.MHCFirestore
import edu.stanford.myheartcounts.firebase.mhcFirebaseLogger
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.grovealliance.account.Account
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountModifications
import org.grovealliance.consent.ConsentResponses
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.foundation.JsonSerializer
import org.grovealliance.markdown.MarkdownDocument
import org.grovealliance.markdown.MarkdownMetadata
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Handles the study's consent form once the participant has filled it in: uploading the signed
 * form as the study's record, and sharing a copy of it.
 *
 * Both work on the consent document in the participant's locale, rendered with their responses.
 * Elements they left untouched carry the initial value the document gives them.
 */
interface MHCConsentHandler {

    /**
     * Uploads the signed form and stamps the consent fields onto the participant's account.
     *
     * The PDF is stored verbatim alongside the document's own metadata and the participant's
     * answers. The account is only updated once the upload succeeds: a participant recorded as
     * consented whose form never arrived is worse than one asked to sign again. A form that arrived
     * without those fields is reported as a failure too.
     */
    suspend fun upload(responses: ConsentResponses): Result<Unit>

    /**
     * Offers the form through the system share sheet, as a PDF named after the document's title.
     */
    suspend fun share(responses: ConsentResponses): Result<Unit>
}

/**
 * Default [MHCConsentHandler] implementation, uploading to Cloud Storage and sharing through the
 * app's file provider.
 */
// One handler for everything done with the signed form; its collaborators are those of the upload
// and of the share.
@Suppress("detekt:LongParameterList")
class MHCConsentHandlerImpl(
    private val documentProvider: MHCConsentDocumentProvider,
    private val formRenderer: MHCConsentFormRenderer,
    private val account: Account,
    private val firestore: MHCFirestore,
    private val timeProvider: TimeProvider,
    private val context: Context,
    private val navigator: Navigator,
    private val ioDispatcher: CoroutineDispatcher,
) : MHCConsentHandler {

    private val logger by mhcFirebaseLogger()

    override suspend fun upload(responses: ConsentResponses): Result<Unit> = runCatchingAsync {
        val accountId = firestore.accountId()
        val document = documentProvider.document()
        val form = formRenderer.render(
            document = document,
            responses = responses,
        )
        val signedAt = timeProvider.nowInstant()
        val version = document.metadata.version?.toString()

        val metadata = storageMetadata {
            contentType = PDF_MIME_TYPE
            setCustomMetadata(FirebaseSchema.ConsentMetadata.CONSENT_FORM_METADATA, encode(metadata = document.metadata))
            setCustomMetadata(FirebaseSchema.ConsentMetadata.RESPONSES, encode(responses = form.responses))
            setCustomMetadata(FirebaseSchema.ConsentMetadata.DATE, DATE_FORMAT.format(signedAt.atOffset(ZoneOffset.UTC)))
            version?.let { setCustomMetadata(FirebaseSchema.ConsentMetadata.VERSION, it) }
        }
        // The file name is the signing time in whole seconds, which is what the read-back listing
        // sorts on.
        FirebaseStorage.getInstance().reference
            .child("${FirebaseSchema.Storage.USERS}/$accountId/${FirebaseSchema.Storage.CONSENT}/${signedAt.epochSecond}.pdf")
            .putBytes(form.pdf, metadata)
            .await()

        recordOnAccount(
            responses = form.responses,
            version = version,
            signedAt = signedAt,
        )
    }.onFailure { throwable ->
        logger.e(throwable) { "Failed to upload the signed consent form." }
    }

    override suspend fun share(responses: ConsentResponses): Result<Unit> = runCatchingAsync {
        val document = documentProvider.document()
        val form = formRenderer.render(
            document = document,
            responses = responses,
        )
        val file = withContext(ioDispatcher) {
            val directory = File(context.cacheDir, SHARED_DIRECTORY).apply { mkdirs() }
            directory.listFiles()?.forEach { it.delete() }
            directory.resolve(sharedFileName(document = document)).apply { writeBytes(form.pdf) }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}$FILE_PROVIDER_SUFFIX", file)
        navigator.push(
            event = NavigationEvent.ShareFile(
                uri = uri.toString(),
                mimeType = PDF_MIME_TYPE,
            ),
        )
    }

    private suspend fun recordOnAccount(
        responses: ConsentResponses,
        version: String?,
        signedAt: Instant,
    ) {
        val modifiedDetails = AccountDetails()
        modifiedDetails[LastSignedConsentDateKey::class] = signedAt
        version?.let { modifiedDetails[LastSignedConsentVersionKey::class] = it }
        modifiedDetails[FutureStudiesKey::class] =
            responses.toggles[MHCConsentDocumentProvider.ID_FUTURE_STUDIES] == true
        modifiedDetails[DidOptInToTrialKey::class] =
            responses.selects[MHCConsentDocumentProvider.ID_TRIAL_OPT_IN] == MHCConsentDocumentProvider.ID_TRIAL_YES
        AccountModifications(modifiedDetails = modifiedDetails)
            .mapCatching { account.service.updateAccountDetails(it).getOrThrow() }
            .getOrThrow()
    }

    private fun sharedFileName(document: MarkdownDocument): String {
        val title = document.metadata.title?.takeIf { it.isNotBlank() } ?: FALLBACK_FILE_NAME
        return "${title.replace(PATH_SEPARATOR, '-')}$PDF_EXTENSION"
    }

    /**
     * The document's frontmatter.
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
        const val PDF_MIME_TYPE = "application/pdf"
        const val PDF_EXTENSION = ".pdf"
        const val SHARED_DIRECTORY = "shared"
        const val FALLBACK_FILE_NAME = "file"
        const val PATH_SEPARATOR = '/'
        const val FILE_PROVIDER_SUFFIX = ".fileprovider"

        /**
         * ISO-8601 in GMT with a dash date separator.
         */
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
    }
}
