//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health

import android.content.Context
import com.google.firebase.storage.FirebaseStorage
import edu.stanford.myheartcounts.firebase.MHCFirestore
import kotlinx.coroutines.tasks.await
import org.grovealliance.core.logging.groveLogger
import java.io.File

/**
 * The Cloud Storage folder an archive belongs in, under the participant's own prefix.
 *
 * The backend keys its ingestion on these names, so they are a contract rather than a naming choice
 * (`onArchivedLiveHealthSampleUploaded.ts:48`).
 */
enum class HealthUploadCategory(val folderName: String) {

    /**
     * Samples measured while the participant has been enrolled.
     */
    LIVE("liveHealthSamples"),

    /**
     * Samples measured before enrollment, backfilled once.
     */
    HISTORICAL("historicalHealthSamples"),

    /**
     * Records of samples the participant deleted from Health Connect after they were uploaded.
     */
    DELETIONS("healthDeletions"),
}

/**
 * Uploads archives to Cloud Storage so that an interrupted upload resumes rather than losing data.
 *
 * An archive is written into a per-category staging folder on disk first and only deleted once
 * Cloud Storage has acknowledged it, so a process death mid-upload leaves the file behind rather
 * than dropping the samples. [recoverOrphans] re-uploads whatever it finds there at launch. Ports
 * iOS's `ManagedFileUpload`.
 */
class ManagedFileUpload(
    private val context: Context,
    private val firestore: MHCFirestore,
) {

    private val logger by groveLogger(tag = "MHCFirebase")

    /**
     * Writes [bytes] into [category]'s staging folder and uploads it.
     *
     * @param fileName The name the archive gets, both on disk and in Cloud Storage.
     * @return A [Result] that succeeds once Cloud Storage holds the archive.
     */
    suspend fun upload(category: HealthUploadCategory, fileName: String, bytes: ByteArray): Result<Unit> {
        val file = File(stagingDirectory(category = category), fileName)
        return runCatching {
            file.writeBytes(bytes)
        }.mapCatching {
            uploadStagedFile(category = category, file = file).getOrThrow()
        }.onFailure { throwable ->
            logger.e(throwable) { "Failed to upload '$fileName' to ${category.folderName}." }
        }
    }

    /**
     * Re-uploads every archive an earlier session left behind.
     *
     * Call this at launch, and only once Firebase is available and the participant is signed in;
     * an orphan belongs to whoever was signed in when it was written.
     */
    suspend fun recoverOrphans() {
        HealthUploadCategory.entries.forEach { category ->
            stagingDirectory(category = category).listFiles().orEmpty().forEach { file ->
                logger.i { "Retrying the interrupted upload of '${file.name}'." }
                uploadStagedFile(category = category, file = file)
            }
        }
    }

    /**
     * Deletes every archive still waiting on disk, for when the participant signs out.
     */
    fun discardStaged() {
        HealthUploadCategory.entries.forEach { category ->
            stagingDirectory(category = category).listFiles().orEmpty().forEach { it.delete() }
        }
    }

    private suspend fun uploadStagedFile(category: HealthUploadCategory, file: File): Result<Unit> =
        runCatching {
            val path = "${USERS_PREFIX}/${firestore.accountId}/${category.folderName}/${file.name}"
            FirebaseStorage.getInstance().reference.child(path).putBytes(file.readBytes()).await()
            // Only now is the archive safe to drop: until Cloud Storage acknowledged it, this file
            // is the only copy of those samples.
            file.delete()
            Unit
        }.onFailure { throwable ->
            logger.e(throwable) { "Upload of '${file.name}' failed; it stays staged for a later retry." }
        }

    private fun stagingDirectory(category: HealthUploadCategory): File =
        File(context.filesDir, "$STAGING_DIRECTORY/${category.folderName}").apply { mkdirs() }

    private companion object {
        const val STAGING_DIRECTORY = "health-uploads"

        /**
         * Matches the `users/{uid}/…` layout `firebasestorage.rules` grants the participant.
         */
        const val USERS_PREFIX = "users"
    }
}
