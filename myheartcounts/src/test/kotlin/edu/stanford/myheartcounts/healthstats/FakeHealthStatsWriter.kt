//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

class FakeHealthStatsWriter : HealthStatsWriter {
    private val contributions = mutableListOf<HealthStatsContribution>()
    private var failure: Exception? = null
    private var afterWrite: (() -> Unit)? = null
    val writes: List<HealthStatsContribution> get() = contributions.toList()

    fun setFailure(error: Exception) {
        failure = error
    }

    fun setAfterWrite(action: () -> Unit) {
        afterWrite = action
    }

    override suspend fun awaitPendingWrites(): Result<Unit> = Result.success(Unit)

    override suspend fun write(contribution: HealthStatsContribution): Result<Unit> {
        failure?.let { return Result.failure(it) }
        contributions += contribution
        afterWrite?.invoke()
        return Result.success(Unit)
    }
}
