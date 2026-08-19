//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import android.content.Context
import edu.stanford.myheartcounts.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.study.StudyManager
import org.grovealliance.studydefinition.StudyBundle
import java.io.File

/**
 * Provides the study bundle shipped with the app.
 *
 * The packaged archive is unpacked into the cache directory on each load, since a bundle must be
 * read from a directory on disk. It is the same archive format the bundle is served in, so a
 * downloaded bundle would unpack the same way.
 */
class MHCStudyBundleProviderImpl(
    private val context: Context,
    private val studyManager: StudyManager,
    private val scope: CoroutineScope,
) : MHCStudyBundleProvider {
    private val logger by groveLogger()
    private val mutex = Mutex()
    private var loaded: StudyBundle? = null
    private var pendingLoad: Deferred<Result<StudyBundle>>? = null

    override fun configure() {
        scope.launch {
            get()
                .mapCatching { bundle ->
                    studyManager.informAboutStudies(listOf(bundle))
                    bundle.studyDefinition.studyRevision
                }
                .onSuccess { revision -> logger.i { "Informed the study manager about revision $revision." } }
                .onFailure { logger.e(it) { "Failed to inform the study manager about the study bundle." } }
        }
    }

    override suspend fun get(): Result<StudyBundle> {
        val load = mutex.withLock {
            loaded?.let { return Result.success(it) }
            pendingLoad ?: scope.async { runCatching { unpack() } }.also { pendingLoad = it }
        }

        val result = load.await()
        mutex.withLock {
            result.onSuccess { loaded = it }
            if (pendingLoad === load) pendingLoad = null
        }

        return result.onFailure { logger.e(it) { "Failed to load the bundled study bundle" } }
    }

    /**
     * Extracts the packaged archive into the cache directory and opens the bundle it holds.
     */
    private fun unpack(): StudyBundle {
        val target = File(context.cacheDir, CACHE_DIRECTORY).resolve(BUNDLE_DIRECTORY_NAME)
        target.parentFile?.mkdirs()
        context.assets.open(BuildConfig.STUDY_BUNDLE_ASSET_PATH).use { archive ->
            StudyBundle.unpack(
                archive = archive,
                bundleDir = target,
            )
        }
        return StudyBundle.open(bundleDir = target)
    }

    private companion object {
        const val CACHE_DIRECTORY = "edu.stanford.myheartcounts/StudyBundles"

        /**
         * The archive is named after the bundle directory it holds, plus the compression suffix.
         */
        const val ARCHIVE_SUFFIX = ".tar.zst"
        val BUNDLE_DIRECTORY_NAME = BuildConfig.STUDY_BUNDLE_ASSET_PATH.removeSuffix(ARCHIVE_SUFFIX)
    }
}
