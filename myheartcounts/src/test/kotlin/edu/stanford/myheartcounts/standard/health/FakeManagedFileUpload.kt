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
 * A [ManagedFileUpload] that records each upload in [uploads], fails them with the error set through
 * [setFailure], and reports the active categories set through [setActiveCategories].
 */
class FakeManagedFileUpload : ManagedFileUpload {
    private val active = MutableStateFlow<Set<HealthUploadCategory>>(emptySet())
    private val recorded = mutableListOf<RecordedUpload>()
    private var failure: Throwable? = null

    /**
     * Every upload that succeeded, in order.
     */
    val uploads: List<RecordedUpload> get() = recorded.toList()

    override val activeCategories: StateFlow<Set<HealthUploadCategory>> = active.asStateFlow()

    override val currentActiveCategories: Set<HealthUploadCategory> get() = active.value

    /**
     * Sets the categories reported as having uploads in flight.
     */
    fun setActiveCategories(categories: Set<HealthUploadCategory>) {
        active.value = categories
    }

    /**
     * Makes every later upload fail with [error], or succeed again when it is `null`.
     */
    fun setFailure(error: Throwable?) {
        failure = error
    }

    override suspend fun upload(
        accountId: String,
        category: HealthUploadCategory,
        fileName: String,
        bytes: ByteArray,
    ): Result<Unit> {
        failure?.let { return Result.failure(it) }
        recorded += RecordedUpload(
            accountId = accountId,
            category = category,
            fileName = fileName,
            bytes = bytes,
        )
        return Result.success(Unit)
    }

    override suspend fun recoverOrphans() = Unit

    override suspend fun discardStaged() = Unit
}

/**
 * One file uploaded through [FakeManagedFileUpload].
 */
data class RecordedUpload(
    val accountId: String,
    val category: HealthUploadCategory,
    val fileName: String,
    val bytes: ByteArray,
)
