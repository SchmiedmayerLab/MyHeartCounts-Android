//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account

/**
 * A [StudyWithdrawal] that returns a configured result and counts its calls.
 */
class FakeStudyWithdrawal : StudyWithdrawal {
    private var result: Result<Unit> = Result.success(Unit)
    private var _withdrawCount = 0

    /**
     * How many times [withdraw] was called.
     */
    val withdrawCount: Int get() = _withdrawCount

    /**
     * Sets what [withdraw] returns.
     */
    fun setResult(result: Result<Unit>) {
        this.result = result
    }

    override suspend fun withdraw(): Result<Unit> {
        _withdrawCount++
        return result
    }
}
