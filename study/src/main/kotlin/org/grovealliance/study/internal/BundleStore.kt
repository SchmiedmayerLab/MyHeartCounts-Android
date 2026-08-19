//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.study.internal

import org.grovealliance.studydefinition.StudyBundle
import java.io.File
import java.util.UUID

/**
 * Stores a private, per-enrollment copy of each study bundle.
 *
 * The manager reads from these copies (rather than a transient download location) so an enrollment
 * keeps working across launches even if the original source is gone.
 */
internal class BundleStore(private val baseDir: File) {

    private fun bundleDir(enrollmentId: UUID): File =
        File(baseDir, "$enrollmentId.${StudyBundle.FILE_EXTENSION}")

    /**
     * Copies [source] into this enrollment's storage location, replacing any existing copy, and
     * returns a bundle handle opened from the stored copy.
     */
    fun store(enrollmentId: UUID, source: StudyBundle): StudyBundle {
        val destination = bundleDir(enrollmentId)
        if (destination.exists()) destination.deleteRecursively()
        destination.parentFile?.mkdirs()
        source.bundleDir.copyRecursively(destination, overwrite = true)
        return StudyBundle.open(destination)
    }

    /**
     * Opens the stored bundle for [enrollmentId], or `null` when none is stored.
     */
    fun open(enrollmentId: UUID): StudyBundle? {
        val dir = bundleDir(enrollmentId)
        return if (dir.isDirectory) StudyBundle.open(dir) else null
    }

    /**
     * Deletes the stored bundle for [enrollmentId], if any.
     */
    fun delete(enrollmentId: UUID) {
        bundleDir(enrollmentId).deleteRecursively()
    }

    /**
     * Deletes any stored bundle directory whose enrollment id is not in [activeEnrollmentIds].
     */
    fun deleteOrphans(activeEnrollmentIds: Set<UUID>) {
        val activeDirNames = activeEnrollmentIds.map { "$it.${StudyBundle.FILE_EXTENSION}" }.toSet()
        baseDir.listFiles()?.forEach { dir ->
            if (dir.isDirectory && dir.name !in activeDirNames) {
                dir.deleteRecursively()
            }
        }
    }
}
