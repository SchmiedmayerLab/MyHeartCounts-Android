//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition

import kotlinx.serialization.Serializable

/**
 * A non-localized reference to a file stored within a [StudyBundle].
 *
 * A reference is resolved against a specific locale via [StudyBundle.resolve], which selects the
 * best-matching localized variant on disk.
 */
@Serializable
data class FileReference(
    val category: Category,
    val filename: String,
    val fileExtension: String,
) {
    /**
     * The classification of a referenced file, determining the sub-directory it lives in.
     */
    @Serializable
    @JvmInline
    value class Category(val rawValue: String) {
        companion object {
            /**
             * Consent documents.
             */
            val consent = Category("consent")

            /**
             * Questionnaire resources.
             */
            val questionnaire = Category("questionnaire")

            /**
             * Informational articles.
             */
            val informationalArticle = Category("article")
        }
    }

    /**
     * The reference in `category/filename.extension` form.
     */
    override fun toString(): String =
        if (fileExtension.isEmpty()) "${category.rawValue}/$filename" else "${category.rawValue}/$filename.$fileExtension"
}
