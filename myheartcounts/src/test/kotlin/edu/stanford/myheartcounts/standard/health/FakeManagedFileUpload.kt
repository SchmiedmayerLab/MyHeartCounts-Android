//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A [ManagedFileUpload] that uploads nothing and reports the active categories set through
 * [setActiveCategories].
 */
class FakeManagedFileUpload : ManagedFileUpload {
    private val active = MutableStateFlow<Set<HealthUploadCategory>>(emptySet())

    override val activeCategories: StateFlow<Set<HealthUploadCategory>> = active.asStateFlow()

    override val currentActiveCategories: Set<HealthUploadCategory> get() = active.value

    /**
     * Sets the categories reported as having uploads in flight.
     */
    fun setActiveCategories(categories: Set<HealthUploadCategory>) {
        active.value = categories
    }

    override suspend fun upload(
        accountId: String,
        category: HealthUploadCategory,
        fileName: String,
        bytes: ByteArray,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun recoverOrphans() = Unit

    override suspend fun discardStaged() = Unit
}
