//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition.fixtures

import edu.stanford.spezi.studydefinition.FileReference

/**
 * Fixture for [FileReference].
 */
object FileReferenceFixtures {
    fun create(
        category: FileReference.Category = FileReference.Category.informationalArticle,
        filename: String = "",
        fileExtension: String = "",
    ): FileReference = FileReference(
        category = category,
        filename = filename,
        fileExtension = fileExtension,
    )
}
