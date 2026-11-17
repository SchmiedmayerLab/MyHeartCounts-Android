//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health

import android.content.Context
import edu.stanford.myheartcounts.firebase.FirebaseSchema
import edu.stanford.myheartcounts.firebase.MHCFirestore
import edu.stanford.myheartcounts.firebase.mhcFirebaseLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.grovealliance.core.coroutines.Concurrency
import org.grovealliance.core.coroutines.runCatchingAsync
import java.io.File

/**
 * Uploads archives to Cloud Storage so that an interrupted upload resumes rather than losing data,
 * and tells which categories have uploads in flight.
 */
interface ManagedFileUpload {

    /**
     * The categories with at least one upload in flight, from the start of an upload, a retried one
     * included, until it succeeds or fails.
     */
    val activeCategories: StateFlow<Set<HealthUploadCategory>>

    /**
     * The categories with at least one upload in flight right now.
     */
    val currentActiveCategories: Set<HealthUploadCategory>

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
    ): Result<Unit>

    /**
     * Re-uploads every archive an earlier session of the signed-in participant left behind, and
     * deletes whatever other participants left behind.
     *
     * Call this at launch, and only once Firebase is available and the participant is signed in.
     */
    suspend fun recoverOrphans()

    /**
     * Deletes every archive still waiting on disk, for when the participant signs out.
     */
    suspend fun discardStaged()
}

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
    LIVE(FirebaseSchema.Storage.LIVE_HEALTH_SAMPLES),

    /**
     * Samples measured before enrollment, backfilled once.
     */
    HISTORICAL(FirebaseSchema.Storage.HISTORICAL_HEALTH_SAMPLES),

    /**
     * Records of samples the participant deleted from Health Connect after they were uploaded.
     */
    DELETIONS(FirebaseSchema.Storage.HEALTH_DELETIONS),
}

/**
 * Default [ManagedFileUpload] implementation.
 *
 * An archive is written into a staging folder on disk first and only deleted once Cloud Storage has
 * acknowledged it, so a process death mid-upload leaves the file behind rather than dropping the
 * samples. [recoverOrphans] re-uploads whatever it finds there at launch.
 *
 * Staging folders are per account. Ownership is the path, so an archive can only ever be uploaded
 * under the participant who produced it — even when the cleanup at sign-out did not get to run.
 */
class ManagedFileUploadImpl(
    private val context: Context,
    private val firestore: MHCFirestore,
    private val concurrency: Concurrency,
) : ManagedFileUpload {

    private val logger by mhcFirebaseLogger()

    /**
     * Serializes access to the staging folders, so recovery cannot pick up an archive that is still
     * being uploaded and send it a second time.
     */
    private val lock = Mutex()

    private val inFlightLock = Any()
    private var inFlight: Map<HealthUploadCategory, Int> = emptyMap()
    private val active = MutableStateFlow<Set<HealthUploadCategory>>(emptySet())

    override val activeCategories: StateFlow<Set<HealthUploadCategory>> = active.asStateFlow()

    override val currentActiveCategories: Set<HealthUploadCategory> get() = active.value

    override suspend fun upload(
        accountId: String,
        category: HealthUploadCategory,
        fileName: String,
        bytes: ByteArray,
    ): Result<Unit> = tracked(category = category) {
        withContext(concurrency.ioDispatcher()) {
            lock.withLock {
                runCatchingAsync {
                    val file = File(stagingDirectory(accountId = accountId, category = category), fileName)
                        .apply { writeBytes(bytes) }
                    uploadStagedFile(accountId = accountId, category = category, file = file).getOrThrow()
                }.onFailure { throwable ->

                    logger.e(throwable) { "Failed to upload '$fileName' to ${category.folderName}." }
                }
            }
        }
    }

    override suspend fun recoverOrphans() {
        withContext(concurrency.ioDispatcher()) {
            lock.withLock {
                val accountId = firestore.accountId()
                // Never theirs to upload, and the cleanup that should have removed them at sign-out
                // did not finish.
                rootDirectory().listFiles().orEmpty()
                    .filter { it.name != accountId }
                    .forEach { it.deleteRecursively() }

                HealthUploadCategory.entries.forEach { category ->
                    stagingDirectory(accountId = accountId, category = category).listFiles().orEmpty().forEach { file ->
                        logger.i { "Retrying the interrupted upload of '${file.name}'." }
                        tracked(category = category) {
                            uploadStagedFile(accountId = accountId, category = category, file = file)
                        }
                    }
                }
            }
        }
    }

    override suspend fun discardStaged() {
        withContext(concurrency.ioDispatcher()) {
            lock.withLock { rootDirectory().deleteRecursively() }
        }
    }

    private suspend fun uploadStagedFile(
        accountId: String,
        category: HealthUploadCategory,
        file: File,
    ): Result<Unit> = runCatchingAsync {
        firestore.userStorageFolder(accountId = accountId)
            .child(category.folderName)
            .child(file.name)
            .putBytes(file.readBytes())
            .await()
        // Only now is the archive safe to drop: until Cloud Storage acknowledged it, this file
        // is the only copy of those samples.
        file.delete()
        Unit
    }.onFailure { throwable ->

        logger.e(throwable) { "Upload of '${file.name}' failed; it stays staged for a later retry." }
    }

    /**
     * Runs [upload], counting it as in flight in [category] until it returns or throws.
     */
    private suspend fun <T> tracked(
        category: HealthUploadCategory,
        upload: suspend () -> T,
    ): T {
        changeInFlight(category = category, delta = 1)
        return try {
            upload()
        } finally {
            changeInFlight(category = category, delta = -1)
        }
    }

    private fun changeInFlight(category: HealthUploadCategory, delta: Int) = synchronized(inFlightLock) {
        val count = (inFlight[category] ?: 0) + delta
        inFlight = if (count > 0) inFlight + (category to count) else inFlight - category
        active.value = inFlight.keys
    }

    private fun rootDirectory(): File = File(context.filesDir, STAGING_DIRECTORY)

    private fun stagingDirectory(accountId: String, category: HealthUploadCategory): File =
        File(rootDirectory(), "$accountId/${category.folderName}").apply { mkdirs() }

    private companion object {
        const val STAGING_DIRECTORY = "health-uploads"
    }
}
