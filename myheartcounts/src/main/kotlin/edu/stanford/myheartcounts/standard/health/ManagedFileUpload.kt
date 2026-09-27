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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.grovealliance.core.coroutines.Concurrency
import org.grovealliance.core.logging.groveLogger
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

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
 * An archive is written into a staging folder on disk first and only deleted once Cloud Storage has
 * acknowledged it, so a process death mid-upload leaves the file behind rather than dropping the
 * samples. [recoverOrphans] re-uploads whatever it finds there at launch. Ports iOS's
 * `ManagedFileUpload`.
 *
 * Staging folders are per account. Ownership is the path, so an archive can only ever be uploaded
 * under the participant who produced it — even when the cleanup at sign-out did not get to run.
 */
class ManagedFileUpload(
    private val context: Context,
    private val firestore: MHCFirestore,
    private val concurrency: Concurrency,
) {

    private val logger by groveLogger(tag = "MHCFirebase")

    /**
     * Serializes access to the staging folders, so recovery cannot pick up an archive that is still
     * being uploaded and send it a second time.
     */
    private val lock = Mutex()

    /**
     * Writes [bytes] into [category]'s staging folder and uploads it.
     *
     * @param accountId The participant the archive belongs to. Passed rather than read from the
     * current account, so data read out of staging for one participant cannot be uploaded as
     * whoever happens to be signed in by the time it is sent.
     * @param fileName The name the archive gets, both on disk and in Cloud Storage.
     * @return A [Result] that succeeds once Cloud Storage holds the archive.
     */
    suspend fun upload(
        accountId: String,
        category: HealthUploadCategory,
        fileName: String,
        bytes: ByteArray,
    ): Result<Unit> = withContext(concurrency.ioDispatcher()) {
        lock.withLock {
            runCatching {
                File(stagingDirectory(accountId = accountId, category = category), fileName)
                    .apply { writeBytes(bytes) }
            }.mapCatching { file ->
                uploadStagedFile(accountId = accountId, category = category, file = file).getOrThrow()
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                logger.e(throwable) { "Failed to upload '$fileName' to ${category.folderName}." }
            }
        }
    }

    /**
     * Re-uploads every archive an earlier session of the signed-in participant left behind, and
     * deletes whatever other participants left behind.
     *
     * Call this at launch, and only once Firebase is available and the participant is signed in.
     */
    suspend fun recoverOrphans() {
        withContext(concurrency.ioDispatcher()) {
            lock.withLock {
                val accountId = firestore.accountId
                // Never theirs to upload, and the cleanup that should have removed them at sign-out
                // did not finish.
                rootDirectory().listFiles().orEmpty()
                    .filter { it.name != accountId }
                    .forEach { it.deleteRecursively() }

                HealthUploadCategory.entries.forEach { category ->
                    stagingDirectory(accountId = accountId, category = category).listFiles().orEmpty().forEach { file ->
                        logger.i { "Retrying the interrupted upload of '${file.name}'." }
                        uploadStagedFile(accountId = accountId, category = category, file = file)
                    }
                }
            }
        }
    }

    /**
     * Deletes every archive still waiting on disk, for when the participant signs out.
     */
    suspend fun discardStaged() {
        withContext(concurrency.ioDispatcher()) {
            lock.withLock { rootDirectory().deleteRecursively() }
        }
    }

    private suspend fun uploadStagedFile(
        accountId: String,
        category: HealthUploadCategory,
        file: File,
    ): Result<Unit> = runCatching {
        val path = "${USERS_PREFIX}/$accountId/${category.folderName}/${file.name}"
        FirebaseStorage.getInstance().reference.child(path).putBytes(file.readBytes()).await()
        // Only now is the archive safe to drop: until Cloud Storage acknowledged it, this file
        // is the only copy of those samples.
        file.delete()
        Unit
    }.onFailure { throwable ->
        if (throwable is CancellationException) throw throwable
        logger.e(throwable) { "Upload of '${file.name}' failed; it stays staged for a later retry." }
    }

    private fun rootDirectory(): File = File(context.filesDir, STAGING_DIRECTORY)

    private fun stagingDirectory(accountId: String, category: HealthUploadCategory): File =
        File(rootDirectory(), "$accountId/${category.folderName}").apply { mkdirs() }

    private companion object {
        const val STAGING_DIRECTORY = "health-uploads"

        /**
         * Matches the `users/{uid}/…` layout `firebasestorage.rules` grants the participant.
         */
        const val USERS_PREFIX = "users"
    }
}
