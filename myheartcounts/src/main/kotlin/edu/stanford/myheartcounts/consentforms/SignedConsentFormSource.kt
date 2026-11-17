//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.consentforms

import android.content.Context
import androidx.core.content.FileProvider
import com.google.firebase.storage.StorageReference
import edu.stanford.myheartcounts.firebase.FirebaseSchema
import edu.stanford.myheartcounts.firebase.MHCFirestore
import edu.stanford.myheartcounts.firebase.mhcFirebaseLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.foundation.JsonSerializer
import java.io.File
import java.time.Instant
import java.time.OffsetDateTime

/**
 * The consent forms the signed-in participant has signed, on any platform.
 */
interface SignedConsentFormSource {

    /**
     * The participant's signed forms, most recently signed first.
     */
    suspend fun forms(): Result<List<SignedConsentForm>>

    /**
     * Copies [form] onto the device, where it stays until [discard]ed.
     */
    suspend fun download(form: SignedConsentForm): Result<DownloadedConsentForm>

    /**
     * Removes a form copied onto the device by [download].
     */
    fun discard(download: DownloadedConsentForm)
}

/**
 * A consent form the participant has signed. Details the form was stored without are `null`.
 *
 * @property id identifies the form among the participant's forms
 * @property title the title of the consent document signed
 * @property version the version of the consent document signed
 * @property signedAt when the form was signed
 */
data class SignedConsentForm(
    val id: String,
    val title: String?,
    val version: String?,
    val signedAt: Instant?,
)

/**
 * A signed [form] copied onto the device as a PDF [file], which other apps can read through
 * [contentUri].
 */
data class DownloadedConsentForm(
    val form: SignedConsentForm,
    val file: File,
    val contentUri: String,
)

/**
 * [SignedConsentFormSource] reading the forms from the participant's folder in Cloud Storage, each
 * described by its stored metadata. A form whose metadata can't be read is listed without details.
 */
class SignedConsentFormSourceImpl(
    private val context: Context,
    private val firestore: MHCFirestore,
    private val ioDispatcher: CoroutineDispatcher,
) : SignedConsentFormSource {

    private val logger by mhcFirebaseLogger()

    override suspend fun forms(): Result<List<SignedConsentForm>> = runCatchingAsync {
        val references = firestore.consentForms().listAll().await().items.sortedByDescending { it.name }
        coroutineScope {
            references.map { async { form(reference = it) } }.awaitAll()
        }
    }.onFailure { logger.e(it) { "Failed to list the signed consent forms." } }

    override suspend fun download(form: SignedConsentForm): Result<DownloadedConsentForm> = runCatchingAsync {
        val file = withContext(ioDispatcher) {
            File(context.cacheDir, DOWNLOAD_DIRECTORY).apply { mkdirs() }.resolve(form.id)
        }
        firestore.consentForms().child(form.id).getFile(file).await()
        DownloadedConsentForm(
            form = form,
            file = file,
            contentUri = FileProvider.getUriForFile(context, "${context.packageName}$FILE_PROVIDER_SUFFIX", file).toString(),
        )
    }.onFailure { logger.e(it) { "Failed to download a signed consent form." } }

    override fun discard(download: DownloadedConsentForm) {
        download.file.delete()
    }

    private suspend fun form(reference: StorageReference): SignedConsentForm {
        val metadata = runCatchingAsync { reference.metadata.await() }
            .onFailure { logger.w(it) { "Failed to read the metadata of a signed consent form." } }
            .getOrNull()
        val document = metadata?.getCustomMetadata(FirebaseSchema.ConsentMetadata.CONSENT_FORM_METADATA)
            ?.let { JsonSerializer.decodeOrNull(text = it, deserializer = MapSerializer(String.serializer(), String.serializer())) }
            .orEmpty()
        return SignedConsentForm(
            id = reference.name,
            title = document[TITLE_KEY],
            version = document[VERSION_KEY],
            signedAt = metadata?.getCustomMetadata(FirebaseSchema.ConsentMetadata.DATE)
                ?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() },
        )
    }

    private companion object {
        const val DOWNLOAD_DIRECTORY = "shared/consent-forms"
        const val FILE_PROVIDER_SUFFIX = ".fileprovider"
        const val TITLE_KEY = "title"
        const val VERSION_KEY = "version"
    }
}
