//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import android.content.Context
import edu.stanford.spezi.core.logging.speziLogger
import edu.stanford.spezi.study.StudyManager
import edu.stanford.spezi.studydefinition.StudyBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Provides the study bundle shipped with the app.
 *
 * The bundle is unpacked from the app's assets into the cache directory on each load, since a
 * bundle must be read from a directory on disk.
 */
class MHCStudyBundleProviderImpl(
    private val context: Context,
    private val studyManager: StudyManager,
    private val scope: CoroutineScope,
) : MHCStudyBundleProvider {
    private val logger by speziLogger()
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
     * Copies the asset bundle into the cache directory and opens it.
     */
    private fun unpack(): StudyBundle {
        val target = File(context.cacheDir, CACHE_DIRECTORY).resolve(STUDY_BUNDLE_ASSET_PATH)
        target.deleteRecursively()
        copyAsset(path = STUDY_BUNDLE_ASSET_PATH, target = target)
        return StudyBundle.open(bundleDir = target)
    }

    /**
     * Copies the asset at [path] to [target], recursing into asset directories.
     */
    private fun copyAsset(path: String, target: File) {
        val children = context.assets.list(path).orEmpty()
        if (children.isEmpty()) {
            target.parentFile?.mkdirs()
            context.assets.open(path).use { source ->
                target.outputStream().use { source.copyTo(it) }
            }
        } else {
            target.mkdirs()
            children.forEach { child ->
                copyAsset(
                    path = "$path/$child",
                    target = File(target, child),
                )
            }
        }
    }

    private companion object {
        const val CACHE_DIRECTORY = "edu.stanford.myheartcounts/StudyBundles"
    }
}
